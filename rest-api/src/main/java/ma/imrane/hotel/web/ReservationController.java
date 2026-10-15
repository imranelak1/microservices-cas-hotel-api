package ma.imrane.hotel.web;
import ma.imrane.hotel.model.*;
import ma.imrane.hotel.data.ReservationRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api")
public class ReservationController {
    private final ReservationRepository repository;
    public ReservationController(ReservationRepository repository){this.repository=repository;}
    @GetMapping("/reservations") public List<Reservation> list(){return repository.list();}
    @GetMapping("/reservations/{id}") public Reservation find(@PathVariable long id){return repository.find(id);}
    @PostMapping("/reservations") @ResponseStatus(HttpStatus.CREATED) public Reservation create(@RequestBody ReservationInput input){return repository.create(input);}
    @PutMapping("/reservations/{id}") public Reservation update(@PathVariable long id,@RequestBody ReservationInput input){return repository.update(id,input);}
    @DeleteMapping("/reservations/{id}") public Map<String,Boolean> delete(@PathVariable long id){return Map.of("deleted",repository.delete(id));}
    @GetMapping("/clients") public List<Map<String,Object>> clients(){return repository.clients();}
    @GetMapping("/chambres") public List<Map<String,Object>> chambres(){return repository.chambres();}
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) public Map<String,String> missing(Exception error){return Map.of("error",error.getMessage());}
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.dao.DataAccessException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(Exception error){return Map.of("error","Réservation invalide ou chambre déjà réservée");}
}
