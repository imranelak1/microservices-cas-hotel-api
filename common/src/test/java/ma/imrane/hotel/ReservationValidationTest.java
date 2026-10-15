package ma.imrane.hotel;

import ma.imrane.hotel.data.ReservationRepository;
import ma.imrane.hotel.model.ReservationInput;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationValidationTest {
    JdbcTemplate jdbc;
    ReservationRepository repository;
    @BeforeEach void setup() throws Exception {jdbc=mock(JdbcTemplate.class);repository=new ReservationRepository(jdbc,new ObjectMapper());}
    ReservationInput input(){return new ReservationInput(1L,1L,"2030-01-01","2030-01-03","calme");}
    @Test void invalidStayDoesNotReachDatabase(){var value=input();value.setDateFin("2029-01-01");assertThrows(IllegalArgumentException.class,()->repository.create(value));verifyNoInteractions(jdbc);}
    @Test void oversizedPayloadDoesNotReachDatabase(){var value=input();value.setPreferences("x".repeat(102401));assertThrows(IllegalArgumentException.class,()->repository.create(value));verifyNoInteractions(jdbc);}
    @Test void missingIdsDoNotReachDatabase(){var value=input();value.setChambreId(null);assertThrows(IllegalArgumentException.class,()->repository.create(value));verifyNoInteractions(jdbc);}
    @Test void malformedDateDoesNotReachDatabase(){var value=input();value.setDateDebut("demain");assertThrows(IllegalArgumentException.class,()->repository.create(value));verifyNoInteractions(jdbc);}
}
