package cl.duoc.andesstay.audit.config;

import cl.duoc.andesstay.audit.security.CognitoJwtAuthenticationConverter;
import cl.duoc.andesstay.audit.security.RestAccessDeniedHandler;
import cl.duoc.andesstay.audit.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * /api/audit es de solo lectura y, segun el caso, solo lo pueden ver los
 * roles Admin y Auditor - nadie mas, ni siquiera un Recepcionista u Huesped.
 *
 * Esta configuracion SOLO se activa cuando el perfil "nosec" NO esta activo,
 * es decir, es la seguridad real con Cognito. Para probar localmente sin
 * tener el User Pool creado todavia, correr con -Dspring-boot.run.profiles=nosec
 * (ver SecurityConfigNoAuth, la version de reemplazo para ese perfil).
 *
 * Los errores 401/403 se responden en JSON (ErrorResponse), igual que el resto
 * de los microservicios.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!nosec")
public class SecurityConfig {

    private final CognitoJwtAuthenticationConverter cognitoJwtAuthenticationConverter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CognitoJwtAuthenticationConverter cognitoJwtAuthenticationConverter,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.cognitoJwtAuthenticationConverter = cognitoJwtAuthenticationConverter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // health: lo usa Docker/EC2. prometheus: lo scrapea Prometheus dentro de la red
                        // interna; NO se debe publicar esta ruta en el API Gateway.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/actuator/prometheus").permitAll()
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(cognitoJwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
