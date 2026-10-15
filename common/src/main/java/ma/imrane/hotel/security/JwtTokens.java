package ma.imrane.hotel.security;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.Date;
@Component
public class JwtTokens {
    private final byte[] secret;
    public JwtTokens(@Value("${hotel.jwt-secret:course7-hotel-local-secret-32-characters-minimum}") String secret) {this.secret=secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);}
    public String create() throws Exception {
        var claims=new JWTClaimsSet.Builder().subject("student").issuer("hotel-lab").expirationTime(new Date(System.currentTimeMillis()+3600000)).issueTime(new Date()).build();
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),claims);jwt.sign(new MACSigner(secret));return jwt.serialize();
    }
    public boolean verify(String token) {
        try {var jwt=SignedJWT.parse(token);var claims=jwt.getJWTClaimsSet();return JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) && jwt.verify(new MACVerifier(secret)) && "hotel-lab".equals(claims.getIssuer()) && claims.getExpirationTime()!=null && claims.getExpirationTime().after(new Date());}
        catch(Exception error){return false;}
    }
}
