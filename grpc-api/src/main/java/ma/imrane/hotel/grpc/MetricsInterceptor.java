package ma.imrane.hotel.grpc;

import io.grpc.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Mesure aussi les appels refusés avant le service métier. */
@GrpcGlobalServerInterceptor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetricsInterceptor implements ServerInterceptor {
    private final MeterRegistry registry;
    public MetricsInterceptor(MeterRegistry registry){this.registry=registry;}
    @Override public <Q,S> ServerCall.Listener<Q> interceptCall(ServerCall<Q,S> call,Metadata headers,ServerCallHandler<Q,S> next){
        var sample=Timer.start(registry);
        var measured=new ForwardingServerCall.SimpleForwardingServerCall<Q,S>(call){
            @Override public void close(Status status,Metadata trailers){
                sample.stop(Timer.builder("hotel.grpc.request").tag("method",call.getMethodDescriptor().getBareMethodName())
                    .tag("status",status.getCode().name()).publishPercentileHistogram().register(registry));
                super.close(status,trailers);
            }
        };
        return next.startCall(measured,headers);
    }
}
