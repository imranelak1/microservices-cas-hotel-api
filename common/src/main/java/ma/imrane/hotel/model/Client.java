package ma.imrane.hotel.model;
        import lombok.*;
        import jakarta.xml.bind.annotation.*;
        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @XmlRootElement @XmlAccessorType(XmlAccessType.FIELD)
        public class Client {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
}
