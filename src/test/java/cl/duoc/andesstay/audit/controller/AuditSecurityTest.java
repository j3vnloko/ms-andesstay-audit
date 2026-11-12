package cl.duoc.andesstay.audit.controller;

import cl.duoc.andesstay.audit.config.CorsConfig;
import cl.duoc.andesstay.audit.config.SecurityConfig;
import cl.duoc.andesstay.audit.repository.AuditEventRepository;
import cl.duoc.andesstay.audit.security.CognitoJwtAuthenticationConverter;
import cl.duoc.andesstay.audit.security.RestAccessDeniedHandler;
import cl.duoc.andesstay.audit.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seguridad real de /api/audit (SecurityConfig, no el perfil nosec):
 *  - sin token -> 401 con cuerpo JSON
 *  - token valido sin rol ADMIN/AUDITOR -> 403 con cuerpo JSON
 *  - token con rol ADMIN o AUDITOR -> 200
 *  - filtros invalidos -> 400 con cuerpo JSON
 *  - preflight CORS desde el frontend permitido
 */
@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, CorsConfig.class, CognitoJwtAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:4200")
class AuditSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AuditEventRepository repository;

    @Test
    void sinTokenResponde401ConJson() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/audit"));
    }

    @Test
    void rolHuespedResponde403ConJson() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_HUESPED"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void rolRecepcionistaResponde403() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolAuditorPuedeConsultar() throws Exception {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/audit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isOk());
    }

    @Test
    void rolAdminPuedeConsultar() throws Exception {
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/audit")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void rangoDeFechasAlRevesResponde400() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .param("desde", "2026-10-02T00:00:00Z")
                        .param("hasta", "2026-10-01T00:00:00Z")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void fechaConFormatoInvalidoResponde400() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .param("desde", "ayer")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parametro 'desde' tiene un formato invalido."));
    }

    @Test
    void sizeFueraDeRangoResponde400() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .param("size", "1000")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rutaInexistenteResponde404ConJsonYNoComo500() throws Exception {
        mockMvc.perform(get("/api/no-existe")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void preflightCorsDelFrontendEsPermitido() throws Exception {
        mockMvc.perform(options("/api/audit")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
