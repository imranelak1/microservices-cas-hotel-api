package ma.imrane.hotel.model;
        import lombok.*;
        import jakarta.xml.bind.annotation.*;
        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @XmlRootElement @XmlAccessorType(XmlAccessType.FIELD)
        public class Chambre {
    private Long id;
    private String type;
    private double prix;
    private boolean disponible;
}
