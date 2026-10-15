package ma.imrane.hotel;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/** Montée progressive ; le mode smoke contrôle le parcours sans comparer les débits. */
public class ReservationSimulation extends Simulation {
    private static final AtomicInteger ROOMS=new AtomicInteger();
    private String body(Session session,String end){
        String start="{\"clientId\":1,\"chambreId\":"+session.getInt("room")+",\"dateDebut\":\"2094-01-01\",\"dateFin\":\""+end+"\",\"preferences\":\"";
        return start+"x".repeat(Math.max(0,Integer.getInteger("size",1)*1024-start.length()-2))+"\"}";
    }
    public ReservationSimulation(){
        var scenario=scenario("Réservation CRUD")
            .exec(session->session.set("room",1+ROOMS.getAndIncrement()%1000))
            .exec(http("login").post("/auth/token").body(StringBody("{\"username\":\"student\",\"password\":\"labpass\"}")).check(status().is(200),jsonPath("$.token").saveAs("jwt")))
            .exec(http("create").post("/api/reservations").header("Authorization","Bearer #{jwt}").body(StringBody(session->body(session,"2094-01-03"))).check(status().is(201),jsonPath("$.id").saveAs("id")))
            .exitHereIfFailed()
            .exec(http("read").get("/api/reservations/#{id}").header("Authorization","Bearer #{jwt}").check(status().is(200)))
            .exec(http("update").put("/api/reservations/#{id}").header("Authorization","Bearer #{jwt}").body(StringBody(session->body(session,"2094-01-04"))).check(status().is(200)))
            .exec(http("delete").delete("/api/reservations/#{id}").header("Authorization","Bearer #{jwt}").check(status().is(200)));
        var protocol=http.baseUrl(System.getProperty("baseUrl","http://localhost:19601")).contentTypeHeader("application/json");
        if(Boolean.getBoolean("smoke"))setUp(scenario.injectOpen(atOnceUsers(2))).protocols(protocol).assertions(global().failedRequests().count().is(0L));
        else setUp(scenario.injectOpen(rampUsers(10).during(Duration.ofSeconds(10)),rampUsers(100).during(Duration.ofSeconds(20)),rampUsers(500).during(Duration.ofSeconds(30)),rampUsers(1000).during(Duration.ofSeconds(40)))).protocols(protocol);
    }
}
