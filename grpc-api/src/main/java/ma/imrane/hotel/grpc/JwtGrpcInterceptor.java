package ma.imrane.hotel.grpc;
import io.grpc.*;
import ma.imrane.hotel.security.JwtTokens;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
@GrpcGlobalServerInterceptor
public class JwtGrpcInterceptor implements ServerInterceptor {
    private final JwtTokens tokens;
    public JwtGrpcInterceptor(JwtTokens tokens){this.tokens=tokens;}
    @Override public <ReqT,RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT,RespT> call,Metadata headers,ServerCallHandler<ReqT,RespT> next){
        String token=headers.get(Metadata.Key.of("authorization",Metadata.ASCII_STRING_MARSHALLER));
        if(token==null || !token.startsWith("Bearer ") || !tokens.verify(token.substring(7))){call.close(Status.UNAUTHENTICATED.withDescription("JWT requis"),new Metadata());return new ServerCall.Listener<>(){};}
        return next.startCall(call,headers);
    }
}
