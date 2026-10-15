package ma.imrane.hotel.benchmark;

import ma.imrane.hotel.proto.*;
import com.fasterxml.jackson.databind.*;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.xml.parsers.DocumentBuilderFactory;

/** Charge réellement les quatre API ; le CSV contient une ligne par appel tenté. */
public class BenchmarkClient {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static String token;
    private static PrintWriter csv;
    private static ManagedChannel channel;
    private static HotelServiceGrpc.HotelServiceBlockingStub grpc;
    private static final String HOST=System.getProperty("host","localhost");
    private static int phaseSequence;
    private record Result(boolean ok,long id,int requestBytes,int responseBytes) { }
    private static int[] numbers(String name,String fallback){return Arrays.stream(System.getProperty(name,fallback).split(",")).mapToInt(Integer::parseInt).toArray();}

    public static void main(String[] args) throws Exception {
        var login=request(19601,"/auth/token","POST","{\"username\":\"student\",\"password\":\"labpass\"}",false);
        token=JSON.readTree(login.body()).get("token").asText();
        channel=ManagedChannelBuilder.forAddress(HOST,Integer.getInteger("grpc.port",19605)).usePlaintext().build();
        var headers=new Metadata();headers.put(Metadata.Key.of("authorization",Metadata.ASCII_STRING_MARSHALLER),"Bearer "+token);
        grpc=HotelServiceGrpc.newBlockingStub(channel).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
        Path path=Path.of(System.getProperty("output","results/raw-client.csv"));Files.createDirectories(path.toAbsolutePath().getParent());
        try(var writer=new PrintWriter(Files.newBufferedWriter(path,StandardCharsets.UTF_8))){
            csv=writer;csv.println("timestamp,protocol,size_kib,concurrency,repetition,operation,elapsed_ms,success,request_bytes,response_bytes");
            for(String protocol:System.getProperty("protocols","rest,soap,graphql,grpc").split(","))
                for(int size:numbers("sizes","1,10,100")) for(int users:numbers("users","10,100,500,1000"))
                    for(int repeat=1;repeat<=Integer.getInteger("repetitions",3);repeat++) phase(protocol,size,users,repeat);
        }finally{channel.shutdownNow().awaitTermination(5,TimeUnit.SECONDS);}
    }

    private static void phase(String protocol,int size,int users,int repetition) throws Exception {
        var pool=executor(users);var gate=new CountDownLatch(1);var futures=new ArrayList<Future<?>>();
        String date=LocalDate.of(2040,1,1).plusDays(++phaseSequence*10L).toString();
        long start=System.nanoTime();
        for(int user=0;user<users;user++){
            final int number=user;
            futures.add(pool.submit(()->{
                try{
                    gate.await();
                    var input=new LinkedHashMap<String,Object>();input.put("clientId",1);input.put("chambreId",number+1);input.put("dateDebut",date);input.put("dateFin",LocalDate.parse(date).plusDays(2).toString());input.put("preferences","");
                    int overhead=JSON.writeValueAsBytes(input).length;input.put("preferences","x".repeat(Math.max(0,size*1024-overhead)));
                    Result created=measure(protocol,size,users,repetition,"create",()->invoke(protocol,"create",0,input));
                    if(!created.ok())return;
                    long id=created.id();
                    measure(protocol,size,users,repetition,"read",()->invoke(protocol,"read",id,input));
                    input.put("dateFin",LocalDate.parse(date).plusDays(3).toString());
                    measure(protocol,size,users,repetition,"update",()->invoke(protocol,"update",id,input));
                    measure(protocol,size,users,repetition,"delete",()->invoke(protocol,"delete",id,input));
                }catch(InterruptedException error){Thread.currentThread().interrupt();}
                catch(Exception error){System.err.println(protocol+" worker: "+error.getClass().getSimpleName()+" "+error.getMessage());}
            }));
        }
        gate.countDown();for(var future:futures)future.get();pool.shutdown();pool.awaitTermination(30,TimeUnit.SECONDS);
        csv.flush();
        System.out.printf(Locale.ROOT,"%s size=%dKiB users=%d repetition=%d duration=%.3fs%n",protocol,size,users,repetition,(System.nanoTime()-start)/1e9);
    }

    /** Java 21 allège le générateur à 1 000 utilisateurs ; les API restent compilées pour Java 17. */
    private static ExecutorService executor(int users) {
        try {return (ExecutorService)Executors.class.getMethod("newVirtualThreadPerTaskExecutor").invoke(null);}
        catch(NoSuchMethodException error){return Executors.newFixedThreadPool(users);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }

    @FunctionalInterface interface Call {Result run() throws Exception;}
    private static Result measure(String protocol,int size,int users,int repetition,String operation,Call call){
        long start=System.nanoTime();Result result;
        try{result=call.run();}catch(Exception error){result=new Result(false,0,0,0);}
        double elapsed=(System.nanoTime()-start)/1e6;
        synchronized(BenchmarkClient.class){csv.printf(Locale.ROOT,"%d,%s,%d,%d,%d,%s,%.6f,%s,%d,%d%n",System.currentTimeMillis(),protocol,size,users,repetition,operation,elapsed,result.ok(),result.requestBytes(),result.responseBytes());}
        return result;
    }

    private static Result invoke(String protocol,String operation,long id,Map<String,Object> input) throws Exception {
        return switch(protocol){case "rest"->rest(operation,id,input);case "soap"->soap(operation,id,input);case "graphql"->graphql(operation,id,input);case "grpc"->grpc(operation,id,input);default->throw new IllegalArgumentException(protocol);};
    }

    private static Result rest(String operation,long id,Map<String,Object> input) throws Exception {
        String body=operation.equals("create")||operation.equals("update")?JSON.writeValueAsString(input):null;
        String method=switch(operation){case "create"->"POST";case "update"->"PUT";case "delete"->"DELETE";default->"GET";};
        var response=request(19601,"/api/reservations"+(operation.equals("create")?"":"/"+id),method,body,true);
        boolean ok=response.statusCode()>=200&&response.statusCode()<300;
        long found=id;if(ok&&operation.equals("create"))found=JSON.readTree(response.body()).get("id").asLong();
        return new Result(ok,found,bytes(body),bytes(response.body()));
    }

    private static Result graphql(String operation,long id,Map<String,Object> input) throws Exception {
        String fields="id clientId chambreId dateDebut dateFin preferences client{id nom prenom email telephone} chambre{id type prix disponible}";
        String query=switch(operation){case "create"->"mutation($input:ReservationInput!){createReservation(input:$input){"+fields+"}}";case "update"->"mutation($id:ID!,$input:ReservationInput!){updateReservation(id:$id,input:$input){"+fields+"}}";case "delete"->"mutation($id:ID!){deleteReservation(id:$id)}";default->"query($id:ID!){reservation(id:$id){"+fields+"}}";};
        var variables=new LinkedHashMap<String,Object>();if(!operation.equals("create"))variables.put("id",Long.toString(id));if(operation.equals("create")||operation.equals("update"))variables.put("input",input);
        String body=JSON.writeValueAsString(Map.of("query",query,"variables",variables));
        var response=request(19603,"/graphql","POST",body,true);JsonNode json=JSON.readTree(response.body());boolean ok=response.statusCode()==200&&!json.has("errors");
        long found=ok&&operation.equals("create")?json.at("/data/createReservation/id").asLong():id;
        return new Result(ok,found,bytes(body),bytes(response.body()));
    }

    private static Result soap(String operation,long id,Map<String,Object> input) throws Exception {
        String name=switch(operation){case "create"->"createReservation";case "update"->"updateReservation";case "delete"->"deleteReservation";default->"getReservation";};
        StringBuilder body=new StringBuilder("<s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/' xmlns:h='http://hotel.imrane.ma/'><s:Body><h:"+name+">");
        if(!operation.equals("create"))body.append("<id>").append(id).append("</id>");
        if(operation.equals("create")||operation.equals("update")){body.append("<input>");for(var field:input.entrySet())body.append('<').append(field.getKey()).append('>').append(field.getValue()).append("</").append(field.getKey()).append('>');body.append("</input>");}
        body.append("</h:").append(name).append("></s:Body></s:Envelope>");
        var response=HTTP.send(HttpRequest.newBuilder(URI.create("http://"+HOST+":19602/ws/hotel")).timeout(Duration.ofSeconds(15)).header("Content-Type","text/xml;charset=UTF-8").header("Authorization","Bearer "+token).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
        boolean ok=response.statusCode()==200&&!response.body().contains(":Fault");long found=id;
        if(ok&&operation.equals("create")){var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);var document=factory.newDocumentBuilder().parse(new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)));found=Long.parseLong(document.getElementsByTagName("id").item(0).getTextContent());}
        return new Result(ok,found,bytes(body.toString()),bytes(response.body()));
    }

    private static Result grpc(String operation,long id,Map<String,Object> input){
        // Une deadline est relative à chaque appel, pas au début de la campagne.
        var call=grpc.withDeadlineAfter(15,TimeUnit.SECONDS);
        var payload=ReservationInput.newBuilder().setClientId(((Number)input.get("clientId")).longValue()).setChambreId(((Number)input.get("chambreId")).longValue()).setDateDebut(input.get("dateDebut").toString()).setDateFin(input.get("dateFin").toString()).setPreferences(input.get("preferences").toString()).build();
        return switch(operation){
            case "create"->{var response=call.createReservation(payload);yield new Result(true,response.getId(),payload.getSerializedSize(),response.getSerializedSize());}
            case "read"->{var request=Id.newBuilder().setId(id).build();var response=call.getReservation(request);yield new Result(true,id,request.getSerializedSize(),response.getSerializedSize());}
            case "update"->{var request=UpdateRequest.newBuilder().setId(id).setInput(payload).build();var response=call.updateReservation(request);yield new Result(true,id,request.getSerializedSize(),response.getSerializedSize());}
            case "delete"->{var request=Id.newBuilder().setId(id).build();var response=call.deleteReservation(request);yield new Result(response.getDeleted(),id,request.getSerializedSize(),response.getSerializedSize());}
            default->throw new IllegalArgumentException(operation);
        };
    }

    private static int bytes(String value){return value==null?0:value.getBytes(StandardCharsets.UTF_8).length;}
    private static HttpResponse<String> request(int port,String path,String method,String body,boolean auth) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://"+HOST+":"+port+path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json");if(auth)builder.header("Authorization","Bearer "+token);
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));return HTTP.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
}
