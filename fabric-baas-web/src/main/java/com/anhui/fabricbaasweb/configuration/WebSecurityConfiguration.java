//package com.anhui.fabricbaasweb.configuration;
//
//import com.anhui.fabricbaasweb.filter.DosFilter;
//import com.anhui.fabricbaasweb.filter.JwtFilter;
//import com.anhui.fabricbaasweb.service.JwtUserDetailsService;
//import com.google.common.collect.Lists;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.ProviderManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.ObjectPostProcessor;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.builders.WebSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//// @PropertySource("classpath:application.yaml")
//@Configuration
//@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
//public class WebSecurityConfiguration {
//    @Autowired
//    private JwtFilter jwtFilter;
//    @Autowired
//    private DosFilter dosFilter;
//    @Autowired
//    private JwtUserDetailsService jwtUserDetailsService;
//    @Value("#{'${spring.security.ant-matchers}'.split(',')}")
//    private String[] antMatchers;
//
//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.authorizeRequests()
//                .and().authorizeRequests()
//                .anyRequest().authenticated()
//                .and()
//                .csrf().disable()
//                .cors().and()
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(dosFilter, JwtFilter.class)
//                .authenticationManager(authenticationManager(jwtUserDetailsService))
//                .userDetailsService(jwtUserDetailsService)
//                .httpBasic(Customizer.withDefaults())
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//
//        return http.build();
//    }
//
////    @Bean
////    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
////        return authenticationConfiguration.getAuthenticationManager();
////    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(UserDetailsService customUserDetailsService) {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(customUserDetailsService);
//        authProvider.setPasswordEncoder(passwordEncoder());
//        List<AuthenticationProvider> providers = Lists.newArrayList(authProvider);
//        return new ProviderManager(providers);
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        List<String> list = new ArrayList<>();
//        Collections.addAll(list,
//                "/favicon.ico",
//                "/swagger-ui/*",
//                "/swagger-resources/**",
//                "/v3/api-docs");
//        Collections.addAll(list, antMatchers);
//        String[] antMatchers = new String[list.size()];
//        list.toArray(antMatchers);
//
//        return web -> {
//            web.ignoring().antMatchers(antMatchers);
//        };
//    }
//}
