package ipron.cloud.web.filter;

import io.jsonwebtoken.Claims;
import ipron.cloud.web.advice.exception.ExpiredTokenException;
import ipron.cloud.web.advice.exception.NotFoundTokenException;
import ipron.cloud.web.advice.exception.UnauthorizedException;
import ipron.cloud.web.cache.CachedGrant;
import ipron.cloud.web.cache.CachedRouteDefinition;
import ipron.cloud.web.service.common.ResponseService;
import ipron.cloud.web.util.JwtUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import wiremock.org.eclipse.jetty.http.HttpParser;

import java.util.Objects;


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

            //whitelist인 경우 인증 통과
            if(!cachedRouteDefinition.isWhiteListURL(exchange)) {
                //쿼리 파라미터의 토큰을 헤더에 넣어준다.
                MultiValueMap<String,String> params = request.getQueryParams();
                if(params.containsKey(queryToken)){
                    String token = params.getFirst(queryToken)!=null ? params.getFirst(queryToken) : "";
                    exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION,token);
                }

                HttpHeaders headers = request.getHeaders();
                //헤더에 인증 정보가 없으면 http 401 리턴
                if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new NotFoundTokenException("Do not exist JWT Token on headers.");
                }

                String jwt = headers.get(HttpHeaders.AUTHORIZATION).get(0).replace("Bearer ", "");
                //JWT가 만료 되었을 때
                if(jwtUtil.isTokenExpired(jwt)){
                    throw new ExpiredTokenException("JWT Token was expired.");
                }else {
                    Claims claims = jwtUtil.extractAllClaims(jwt);
                    //인가 정보가 허용되지 않을때
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
