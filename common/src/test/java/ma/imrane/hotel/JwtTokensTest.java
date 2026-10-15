package ma.imrane.hotel;

import ma.imrane.hotel.security.JwtTokens;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

class JwtTokensTest {
    static final String SECRET="course7-hotel-local-secret-32-characters-minimum";
    JwtTokens tokens=new JwtTokens(SECRET);
    @Test void signedTokenIsAccepted() throws Exception {assertTrue(tokens.verify(tokens.create()));}
    @Test void tamperedTokenIsRejected() throws Exception {String value=tokens.create();assertFalse(tokens.verify(value.substring(0,value.length()-5)+"aaaaa"));}
    @Test void expiredTokenIsRejected() throws Exception {
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),new JWTClaimsSet.Builder().subject("student").issuer("hotel-lab").expirationTime(new Date(0)).build());
        jwt.sign(new MACSigner(SECRET));assertFalse(tokens.verify(jwt.serialize()));
    }
    @Test void unsignedOrMalformedTokensAreRejected() {assertFalse(tokens.verify("invalid"));assertFalse(tokens.verify("eyJhbGciOiJub25lIn0.e30."));}
}
