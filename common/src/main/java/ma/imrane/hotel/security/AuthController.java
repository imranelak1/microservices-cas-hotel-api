package ma.imrane.hotel.security;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;
/** Compte local du TP uniquement ; aucun fournisseur OAuth2 n'est simulé. */
@RestController
public class AuthController {
    private final JwtTokens tokens;
    public AuthController(JwtTokens tokens){this.tokens=tokens;}
    @PostMapping("/auth/token") public Map<String,String> token(@RequestBody Map<String,String> input) throws Exception {
        if(!"student".equals(input.get("username")) || !"labpass".equals(input.get("password"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return Map.of("token",tokens.create());
    }
}
