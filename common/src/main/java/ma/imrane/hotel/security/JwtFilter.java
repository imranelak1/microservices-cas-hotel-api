package ma.imrane.hotel.security;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtTokens tokens;
    public JwtFilter(JwtTokens tokens){this.tokens=tokens;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        response.setHeader("Access-Control-Allow-Origin","http://localhost:13082");response.setHeader("Access-Control-Allow-Headers","Authorization,Content-Type");response.setHeader("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS");
        if("OPTIONS".equals(request.getMethod())) {response.setStatus(204);return;}
        String path=request.getRequestURI();
            String query=request.getQueryString();
            boolean contract="GET".equals(request.getMethod()) && path.equals("/ws/hotel") && query!=null && (query.equalsIgnoreCase("wsdl") || query.startsWith("xsd="));
            if(path.startsWith("/actuator/") || path.equals("/auth/token") || contract) {chain.doFilter(request,response);return;}
        String value=request.getHeader("Authorization");
        if(value==null || !value.startsWith("Bearer ") || !tokens.verify(value.substring(7))) {response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"error\":\"JWT requis\"}");return;}
        chain.doFilter(request,response);
    }
}
