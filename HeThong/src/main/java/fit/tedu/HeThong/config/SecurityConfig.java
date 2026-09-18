package fit.tedu.HeThong.config;

import fit.tedu.HeThong.security.JwtAuthFilter;
import fit.tedu.HeThong.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - Static resources and auth
                .requestMatchers("/", "/index.html", "/register.html").permitAll()
                .requestMatchers("/login.html", "/posts.html").permitAll()
                // HTML pages are protected by the client-side role guard; APIs remain role-protected below.
                .requestMatchers("/css/**", "/js/**", "/admin/**", "/teacher/**", "/accountant/**", "/user/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers("/api/user/accounts/**").hasAuthority("ADMIN")
                .requestMatchers("/api/archive/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers("/api/tuition-payments/class/*/close").hasAnyAuthority("ADMIN","ACCOUNTANT","MANAGER")
                .requestMatchers("/api/teaching-sessions/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS")
                // Students - Only admin and teacher can view list, students cannot
                .requestMatchers(HttpMethod.GET, "/api/students/me").authenticated() // Students can view their own info
                .requestMatchers(HttpMethod.GET, "/api/students").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/students/class/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","MANAGER","STUDENT")
                .requestMatchers(HttpMethod.POST,   "/api/students/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/students/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/students/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/api/students/*/remove-from-class").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                // Teachers - All can view, only admin can modify
                .requestMatchers(HttpMethod.GET, "/api/teachers/me").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/teachers/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.POST,   "/api/teachers").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/teachers/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/teachers/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                // Classes - Teacher and Admin can view, only Admin can modify
                .requestMatchers(HttpMethod.GET, "/api/classes/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","MANAGER","STUDENT")
                .requestMatchers(HttpMethod.POST,   "/api/classes").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS")
                .requestMatchers(HttpMethod.PUT,    "/api/classes/*/tuition-fee").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","ACCOUNTANT")
                .requestMatchers(HttpMethod.PUT,    "/api/classes/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/classes/**").hasAuthority("ADMIN")
                // Tuition payments - students view own history, accountant/admin manage class histories
                .requestMatchers(HttpMethod.GET, "/api/tuition-payments/me").hasAnyAuthority("STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/tuition-payments/student/**").hasAnyAuthority("ADMIN","ACCOUNTANT","MANAGER","STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/tuition-payments/class/**").hasAnyAuthority("ADMIN","ACCOUNTANT","MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/tuition-payments/student/*/collect").hasAnyAuthority("ADMIN","ACCOUNTANT","MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/tuition-payments/**").authenticated()
                // Video posts
                .requestMatchers(HttpMethod.POST, "/api/posts/**").hasAnyAuthority("ADMIN","CONTENT_MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/posts/**").hasAnyAuthority("ADMIN","CONTENT_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/posts/**").hasAnyAuthority("ADMIN","CONTENT_MANAGER")
                // Schedules - All authenticated users can view, only Admin can modify
                .requestMatchers(HttpMethod.GET, "/api/schedules/my").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","MANAGER","STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/schedules/class/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","MANAGER","STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/schedules/teacher/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/schedules/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","MANAGER","STUDENT")
                .requestMatchers(HttpMethod.POST,   "/api/schedules").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/schedules/**").hasAnyAuthority("ADMIN","ACADEMIC_AFFAIRS","MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/schedules/**").hasAuthority("ADMIN")
                // Attendances - students/teachers/admin can view history, only teacher/admin can mark
                .requestMatchers(HttpMethod.GET, "/api/attendances/student/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/attendances/schedule/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS")
                .requestMatchers(HttpMethod.GET, "/api/attendances/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS","STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/attendances/**").hasAnyAuthority("ADMIN","TEACHER","ACADEMIC_AFFAIRS")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
