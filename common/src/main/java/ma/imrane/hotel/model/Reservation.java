package ma.imrane.hotel.model;
        import lombok.*;
        import jakarta.xml.bind.annotation.*;
        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @XmlRootElement @XmlAccessorType(XmlAccessType.FIELD)
        public class Reservation {
    private Long id;
    private Long clientId;
    private Long chambreId;
    private String dateDebut;
    private String dateFin;
    private String preferences;
    private Client client;
    private Chambre chambre;
}
