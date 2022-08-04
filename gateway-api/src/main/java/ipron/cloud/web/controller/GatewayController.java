package ipron.cloud.web.controller;

import ipron.cloud.web.cache.CachedGrant;
import ipron.cloud.web.cache.CachedRouteDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private CachedRouteDefinition cachedRouteDefinition;
    private CachedGrant cachedGrant;

    public GatewayController(CachedRouteDefinition cachedRouteDefinition, CachedGrant cachedGrant) {
        this.cachedRouteDefinition = cachedRouteDefinition;
        this.cachedGrant = cachedGrant;
    }

    @GetMapping("/routes")
    public Mono<Map> getRouteInfo(){
        return cachedRouteDefinition.getRoutes();
    }

    @GetMapping("/grants")
    public Mono<Map> getGrantInfo(){
        return cachedGrant.getGrantsByTenent();
    }
}
