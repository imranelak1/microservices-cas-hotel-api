package ma.imrane.hotel.data;
import ma.imrane.hotel.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.util.*;
/** Même fichier SQL que le serveur Apollo ; seuls les placeholders JDBC sont adaptés. */
@Repository
public class ReservationRepository {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(ReservationRepository.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Map<String,String> sql;
    public ReservationRepository(JdbcTemplate jdbc,ObjectMapper mapper) throws Exception {
        this.jdbc=jdbc;this.mapper=mapper;
        try(var source=getClass().getResourceAsStream("/queries.json")) {sql=mapper.readValue(source,new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});}
    }
    private String query(String name) {return sql.get(name).replaceAll("\\$[0-9]+","?");}
    public Reservation find(long id) {var result=jdbc.query(query("find"),(rs,n)->map(rs),id).stream().findFirst().orElseThrow(()->new NoSuchElementException("Réservation introuvable"));LOG.info("reservation read id={}",id);return result;}
    public List<Reservation> list() {return jdbc.queryForList(query("list"),Long.class).stream().map(this::find).toList();}
    public List<Map<String,Object>> clients() {return jdbc.queryForList(query("clients"));}
    public List<Map<String,Object>> chambres() {return jdbc.queryForList(query("chambres"));}
    @Transactional
    public Reservation create(ReservationInput input) {
        validate(input);
        // Le paramètre chambreId est répété car le SQL commun vérifie la disponibilité.
        Long id=jdbc.queryForObject(query("create"),Long.class,input.getClientId(),input.getChambreId(),input.getDateDebut(),input.getDateFin(),input.getPreferences(),input.getChambreId());
        return find(id);
    }
    @Transactional
    public Reservation update(long id,ReservationInput input) {
        validate(input);
        jdbc.queryForObject(query("update"),Long.class,input.getClientId(),input.getChambreId(),input.getDateDebut(),input.getDateFin(),input.getPreferences(),id);
        return find(id);
    }
    public boolean delete(long id) {return !jdbc.queryForList(query("delete"),Long.class,id).isEmpty();}
    private void validate(ReservationInput input) {
        if(input.getClientId()==null || input.getChambreId()==null) throw new IllegalArgumentException("Client et chambre requis");
        if(input.getDateDebut()==null || input.getDateFin()==null) throw new IllegalArgumentException("Dates requises");
        java.time.LocalDate start,end;
        try {start=java.time.LocalDate.parse(input.getDateDebut());end=java.time.LocalDate.parse(input.getDateFin());}
        catch(java.time.DateTimeException error) {throw new IllegalArgumentException("Dates ISO invalides",error);}
        if(!end.isAfter(start)) throw new IllegalArgumentException("Le départ doit suivre l'arrivée");
        if(input.getPreferences()==null) input.setPreferences("");
        if(input.getPreferences().length()>102400) throw new IllegalArgumentException("Préférences limitées à 100 Ko");
    }
    private Reservation map(ResultSet rs) throws java.sql.SQLException {
        try {return new Reservation(rs.getLong("id"),rs.getLong("clientId"),rs.getLong("chambreId"),rs.getString("dateDebut"),rs.getString("dateFin"),rs.getString("preferences"),mapper.readValue(rs.getString("client"),Client.class),mapper.readValue(rs.getString("chambre"),Chambre.class));}
        catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new java.sql.SQLException(e);}
    }
}
