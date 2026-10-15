package ma.imrane.hotel.soap;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.*;
@Configuration
public class CxfConfig {
    @Bean EndpointImpl endpoint(Bus bus,HotelService service){var endpoint=new EndpointImpl(bus,service);endpoint.publish("/hotel");return endpoint;}
}
