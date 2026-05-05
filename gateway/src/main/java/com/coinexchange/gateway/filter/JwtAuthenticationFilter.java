package com.coinexchange.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Set<String> PERMIT_PATHS = Set.of(
            "/api/users/login",
            "/api/users/sign-up"
    );

    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (PERMIT_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null) {
            return reject(exchange, "토큰 누락");
        }

        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange, "유효하지 않은 토큰: " + e.getMessage());
        }

        ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", claims.get("role", String.class))
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String extractToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst("accessToken");
        return cookie != null ? cookie.getValue() : null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        log.warn("인증 거부: path={}, reason={}", exchange.getRequest().getPath(), reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
