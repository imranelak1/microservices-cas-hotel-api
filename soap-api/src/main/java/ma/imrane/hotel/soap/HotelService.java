package ma.imrane.hotel.soap;
import ma.imrane.hotel.model.*;
import ma.imrane.hotel.data.ReservationRepository;
import jakarta.jws.*;
import org.springframework.stereotype.Component;
import java.util.List;
@Component @WebService(serviceName="HotelService",targetNamespace="http://hotel.imrane.ma/")
public class HotelService {
    private final ReservationRepository repository;
    public HotelService(ReservationRepository repository){this.repository=repository;}
    @WebMethod public Reservation createReservation(@WebParam(name="input") ReservationInput input){return repository.create(input);}
    @WebMethod public Reservation getReservation(@WebParam(name="id") long id){return repository.find(id);}
    @WebMethod public Reservation updateReservation(@WebParam(name="id") long id,@WebParam(name="input") ReservationInput input){return repository.update(id,input);}
    @WebMethod public boolean deleteReservation(@WebParam(name="id") long id){return repository.delete(id);}
    @WebMethod public List<Reservation> listReservations(){return repository.list();}
}
