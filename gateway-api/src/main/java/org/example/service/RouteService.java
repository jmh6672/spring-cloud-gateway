package org.example.service;

import org.example.entity.GatewayRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
@Slf4j
public class RouteService {

    private boolean routeOnChangeEvent;

    private final String COLLECTION = "gatewayRoute";

    private ReactiveMongoTemplate mongoTemplate;

    public RouteService(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 라우트 목록 조회*/
    public Flux<GatewayRoute> getRoutes(){
        return mongoTemplate.findAll(GatewayRoute.class);
    }

    /**
     * 라우트 변경 스트림 리스너
     * */
    public Flux<ChangeStreamEvent<GatewayRoute>> routeChangeStreamListner(){
        if(routeOnChangeEvent) {
            log.warn("Aleady route streaming.");
            return Flux.empty();
        }

        return this.mongoTemplate.changeStream(GatewayRoute.class)
                .watchCollection(COLLECTION)
                .listen()
                .doOnSubscribe(subscription -> {
                    log.info("Start route change stream!");
                    routeOnChangeEvent=true;
                })
                .doOnTerminate(() -> {
                    log.info("Terminate route change stream!");
                    routeOnChangeEvent=false;
                })
                .onErrorContinue(
                      throwable -> true
                      , (throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage())
                );
    }

}
