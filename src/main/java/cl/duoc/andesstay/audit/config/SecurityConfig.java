package cl.duoc.andesstay.audit.config;

import cl.duoc.andesstay.audit.security.CognitoJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!nosec")
public class SecurityConfig {

    private final CognitoJwtAuthenticationConverter cognitoJwtAuthenticationConverter;

    public SecurityConfig(CognitoJwtAuthenticationConverter cognitoJwtAuthenticationConverter) {
        this.cognitoJwtAuthenticationConverter = cognitoJwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(cognitoJwtAuthenticationConverter)));

        return http.build();
    }
}
