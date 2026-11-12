package cl.duoc.andesstay.audit.security;

import cl.duoc.andesstay.audit.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Se ejecuta cuando la peticion no trae un JWT valido (sin token, expirado,
 * firma invalida, issuer o client_id incorrectos). Responde 401 con el mismo
 * cuerpo JSON que usan el BFF, catalog y reservations.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String message = "Token invalido o no proporcionado.";
        if (authException instanceof OAuth2AuthenticationException oAuth2Exception
                && oAuth2Exception.getError() != null
                && oAuth2Exception.getError().getDescription() != null) {
            message = oAuth2Exception.getError().getDescription();
        }

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(), "unauthorized", message, request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
