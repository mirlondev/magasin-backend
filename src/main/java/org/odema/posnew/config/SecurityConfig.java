package org.odema.posnew.config;


import lombok.RequiredArgsConstructor;
import org.odema.posnew.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/inventory/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER", "CASHIER")
                        // API Employees
                        .requestMatchers(HttpMethod.POST, "/api/employees/store/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/employees/warehouse/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/employees/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PUT, "/api/employees/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/employees/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.POST, "/api/customers/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PATCH, "/api/customers/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER")

                        // API Orders
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER")
                        .requestMatchers("/receipts/**").authenticated()

                        // API Refunds
                        .requestMatchers(HttpMethod.GET, "/api/refunds/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.POST, "/api/refunds/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PATCH, "/api/refunds/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.DELETE, "/api/refunds/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER")

                        // API Shift Reports
                        .requestMatchers(HttpMethod.GET, "/api/shift-reports/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.POST, "/api/shift-reports/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PUT, "/api/shift-reports/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PATCH, "/api/shift-reports/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER")

                        //files

                                .requestMatchers("/uploads/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER")

                        //files invoices
                        .requestMatchers(HttpMethod.POST, "/products/*/image/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/products/*/image/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/products/*/image/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER")
                        .requestMatchers(HttpMethod.GET, "//products/*/image/**").hasAnyRole(
                                "ADMIN", "DEPOT_MANAGER", "SHOP_MANAGER", "CASHIER", "EMPLOYEE")

                        // API Factures
                        .requestMatchers(HttpMethod.POST, "/invoices/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/invoices/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.PATCH, "/invoices/**").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/invoices/*/pdf").hasAnyRole(
                                "ADMIN", "SHOP_MANAGER", "CASHIER")

                                .anyRequest().authenticated()


                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider( userDetailsService) ;
        //authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:4300"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}