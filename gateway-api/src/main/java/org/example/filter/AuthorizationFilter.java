package org.example.filter;

import io.jsonwebtoken.Claims;
import org.example.advice.exception.ExpiredTokenException;
import org.example.advice.exception.NotFoundTokenException;
import org.example.advice.exception.UnauthorizedException;
import org.example.cache.CachedGrant;
import org.example.cache.CachedRouteDefinition;
import org.example.service.common.ResponseService;
import org.example.util.JwtUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;


@Component
@Slf4j
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> implements Ordered {

    private String queryToken;
    private JwtUtil jwtUtil;
    private ResponseService responseService;
    private CachedRouteDefinition cachedRouteDefinition;
    private CachedGrant cachedGrant;

    public AuthorizationFilter(@Value("${spring.jwt.queryToken}") String queryToken,
                               JwtUtil jwtUtil,
                               ResponseService responseService,
                               CachedRouteDefinition routeDefinition,
                               CachedGrant grant) {
        super(Config.class);
        this.queryToken = queryToken;
        this.jwtUtil = jwtUtil;
        this.responseService = responseService;
        this.cachedRouteDefinition = routeDefinition;
        this.cachedGrant = grant;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            log.info("Request URI -> {}", request.getURI().getPath());

            //whitelist??? ?????? ?????? ??????
            if(!cachedRouteDefinition.isWhiteListURL(exchange)) {
                //?????? ??????????????? ????????? ????????? ????????????.
                MultiValueMap<String,String> params = request.getQueryParams();
                if(params.containsKey(queryToken)){
                    String token = params.getFirst(queryToken)!=null ? params.getFirst(queryToken) : "";
                    exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION,token);
                }

                HttpHeaders headers = request.getHeaders();
                //????????? ?????? ????????? ????????? http 401 ??????
                if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new NotFoundTokenException("Do not exist JWT Token on headers.");
                }

                String jwt = headers.get(HttpHeaders.AUTHORIZATION).get(0).replace("Bearer ", "");
                //JWT??? ?????? ????????? ???
                if(jwtUtil.isTokenExpired(jwt)){
                    throw new ExpiredTokenException("JWT Token was expired.");
                }else {
                    Claims claims = jwtUtil.extractAllClaims(jwt);
                    //?????? ????????? ???????????? ?????????
                    if(!cachedGrant.isGrantValid(exchange,claims.get("tntId").toString(),claims.get("authLevel").toString())) {
                        throw new UnauthorizedException("Do not have permission that grant in JWT payload.");
                    }
                }
            }

            if(config.isPreLogger()) {
                log.info("Global Filter Start: request id -> {}", request.getId());
            }

            // Custom Post Filter
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();

                if(config.isPostLogger()) {
                    log.info("Response -> {}", response.getStatusCode());
                }
            }));
        };
    }

    @Override
    public int getOrder() {
        return 0;
    }

    // Put the configuration properties
    @Getter
    @Setter
    @Builder
    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
