
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

import java.util.Objects;


@Component
@Slf4j
public class GrafanaFilter extends AbstractGatewayFilterFactory<GrafanaFilter.Config> implements Ordered {

    public GrafanaFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

            // Grafana UI 접속시 jwt-token 내용 삭제
            exchange.getRequest().mutate().header(HttpHeaders.AUTHORIZATION, "");
            exchange.getRequest().mutate().header("X-WEBAUTH-USER", "B-CLOUD");

            // Custom Post Filter
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();
            }));
        };
    }

    @Override
    public int getOrder() {
        return 1;
    }

    // Put the configuration properties
    @Getter
    @Setter
    @Builder
    public static class Config {
    }
}
