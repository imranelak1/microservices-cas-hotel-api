package ma.imrane.hotel.model;
import lombok.*;
import jakarta.xml.bind.annotation.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class ReservationInput {
    private Long clientId;
    private Long chambreId;
    private String dateDebut;
    private String dateFin;
    private String preferences;
}
