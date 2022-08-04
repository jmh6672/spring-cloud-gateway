//package ipron.cloud.web.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
//import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsConfigurationSource;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//
//@Order(99)
//@Configuration
//@EnableWebFluxSecurity
//public class SecurityConfig {
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity, ReactiveClientRegistrationRepository clientRegistrationRepository){
//        httpSecurity
//                // csrf 요청변조를 허용해야 할지도 모른다.
//                .csrf().disable()
//                .cors().configurationSource(corsConfigurationSource())
////                .and()
////                .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec.anyExchange().authenticated())
////                .oauth2Login(Customizer.withDefaults())
////                .logout(logoutSpec -> logoutSpec.logoutSuccessHandler(
////                        new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository)
////                ))
//                ;
//
//
//        return httpSecurity.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration corsConfiguration = new CorsConfiguration();
//
//        corsConfiguration.addAllowedOriginPattern("*");
//
//        corsConfiguration.addAllowedHeader("text/event-stream");
//        corsConfiguration.addAllowedHeader("x-requested-with");
//        corsConfiguration.addAllowedHeader("authorization");
//        corsConfiguration.addAllowedHeader("content-type");
//        corsConfiguration.addAllowedHeader("credential");
//        corsConfiguration.addAllowedHeader("X-AUTH-TOKEN");
//        corsConfiguration.addAllowedHeader("X-CSRF-TOKEN");
//
//        corsConfiguration.addAllowedMethod("POST");
//        corsConfiguration.addAllowedMethod("GET");
//        corsConfiguration.addAllowedMethod("PUT");
//        corsConfiguration.addAllowedMethod("OPTIONS");
//        corsConfiguration.addAllowedMethod("DELETE");
//        corsConfiguration.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", corsConfiguration);
//
//        return source;
//    }
//}
