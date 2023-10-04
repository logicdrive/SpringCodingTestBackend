package com.toy.codingtest.components.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@Configuration
// @RequiredArgsConstructor
public class SecurityConfig {
    // JWT 유효성 검사에 사용될 RSA 암호화 관련 공개키, 개인키를 특정 파일경로에서 불러오기 위해서
    @Value("${jwt.public.key}")
    private RSAPublicKey rsaPublicKey;
  
    @Value("${jwt.private.key}")
    private RSAPrivateKey rsaPrivateKey;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 개발용 버전을 위해서 보안 요소를 비활성화시키기 위해서
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())

            
            .authorizeHttpRequests(request -> request
                // SignIn, SignUp 요청을 모든 유저로부터 허용시키기 위해서
                .antMatchers(HttpMethod.POST, "/users").permitAll()
                .antMatchers(HttpMethod.GET, "/users").permitAll()

                // Swagger URL 들을 전부 예외 처리시키기 위해서
                .antMatchers(
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // H2 서버 URL 들을 전부 예외 처리시키기 위해서
                .antMatchers(
                    "/h2-console/**"
                ).permitAll()

                // 다른 모든 요청들에 대한 접근에 권한 검사를 하기 위해서
                .anyRequest().authenticated()
            )

            // H2 서버 관련 XFrame 보안 이슈를 해결하기 위해서 같은 URL에 대해서는 XFrame이 허용되도록 만듬
            .headers(header -> header
                .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
            )

            
            // JWT 관련 설정을 추가시키기 위해서
            // JWT 를 쓰므로, 필요없는 세션 관리를 비활성화시키기 위해서
            .sessionManagement(management -> management
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // JWT 인증 실패시에 실패 메세지를 보내기 위해서
            .exceptionHandling(handle -> handle
                .authenticationEntryPoint((request, response, ex) -> {
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        ex.getMessage()
                    );
                })
            );
            
            // TODO : JWT 인증을 위한 토큰 필터를 기본 인증 필터 앞에 추가시키기 위해서
            // .addFilterBefore(
            //     jwtTokenFilter,
            //     UsernamePasswordAuthenticationFilter.class
            // );
 
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


    // JWT 관리를 위한 노출 함수들
    // AuthenticationManager을 주입을 통해서 사용할 수 있도록 만들기 위해서
    @Bean
    public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
      return authenticationConfiguration.getAuthenticationManager();
    }

    // RSA 암호화를 이용해서 JWT 토큰의 유효성을 검증하기 위한 인코더 & 디코더
    @Bean
    public JwtEncoder jwtEncoder() {
        var jwk = new RSAKey.Builder(this.rsaPublicKey).privateKey(this.rsaPrivateKey).build();
        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.rsaPublicKey).build();
    }
}
