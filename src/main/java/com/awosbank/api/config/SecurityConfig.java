package com.awosbank.api.config;

import com.awosbank.api.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authProvider())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/css/**", "/js/**", "/images/**").permitAll();
                    auth.requestMatchers("/login", "/cadastro").permitAll();
                    // healthcheck do Docker/orquestrador (só /health é exposto — ver application.yml)
                    auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    // Swagger/OpenAPI: nos perfis em que está desligado (prod) estes caminhos retornam 404
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    auth.anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginPage("/login").loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error").permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll()
                )
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .headers(h -> h.frameOptions(f -> f.deny()));
        return http.build();
    }
}