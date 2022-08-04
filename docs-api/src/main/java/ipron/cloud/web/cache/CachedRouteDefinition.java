package ipron.cloud.web.cache;


import com.fasterxml.jackson.databind.ObjectMapper;
import ipron.cloud.web.entity.GatewayRoute;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

import static org.springdoc.core.Constants.API_DOCS_URL;

@Slf4j
@Component
public class CachedRouteDefinition implements RouteDefinitionRepository, ApplicationEventPublisherAware {

    private String COLLECTION = "gatewayRoute";

   	@Value(API_DOCS_URL) String API_URL;

    private Map<String, RouteDefinition> routes = Collections.synchronizedMap(new LinkedHashMap<>());

    private ApplicationEventPublisher publisher;

    private ReactiveMongoTemplate mongoTemplate;

    private ObjectMapper objectMapper;

    private RestartEndpoint restartEndpoint;

    Disposable routeChangeDisposalbe;

    public CachedRouteDefinition(ReactiveMongoTemplate mongoTemplate, ObjectMapper objectMapper,RestartEndpoint restartEndpoint) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.restartEndpoint = restartEndpoint;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }


    /**
     * 초기 라우트 정보 가져오기
     * */
    @SneakyThrows
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        if(Objects.nonNull(routes) && routes.size() > 0){
            return Flux.fromIterable(routes.values());
        }

        routeChangeDisposalbe = mongoTemplate.changeStream(GatewayRoute.class)
                .watchCollection(COLLECTION)
                .listen()
                .onErrorContinue(
                      throwable -> true
                      , (throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage())
                )
                .doOnSubscribe(subscription -> log.info("Start route change stream!"))
                .doOnTerminate(() -> log.info("End route change stream!"))
                .doOnNext(changeStreamEvent -> {
                    restartApplication();
                })
                .subscribe();


        Flux<GatewayRoute> routeDefinitions = mongoTemplate.findAll(GatewayRoute.class, COLLECTION);

        routeDefinitions.toStream()
            .filter(routeEntity -> (!routeEntity.getRouteId().contains("docs") && BooleanUtils.isTrue(routeEntity.getEnableYn())))
            .forEach(routeEntity -> {
                routes.put(routeEntity.get_id().toHexString(),new RouteDefinition(){{
                    setId(routeEntity.getRouteId());

                    //RewritePath 설정을 추가한다.
                    List<FilterDefinition> filterDefinitionList = new ArrayList(){{
                        add(new FilterDefinition("RewritePath") {{
                            setArgs(new HashMap() {{
                                put("regexp", "/(?<path>.*)"+API_URL);
                                put("replacement", API_URL);
                            }});
                        }});
                    }};
                    Optional.ofNullable(routeEntity.getFilters()).ifPresent(n -> {
                        List<FilterDefinition> filters = routeEntity.getFilters();
                        filters.stream().filter(f -> f.getName().equals("RewritePath"))
                                .forEach(f -> filterDefinitionList.add(f));
                    });
                    setFilters(filterDefinitionList);

                    //Route proxy 조건에 routeId Path도 추가한다.
//                    List<PredicateDefinition> predicateDefinitionList = new ArrayList<>(){{
//                        add(new PredicateDefinition("Path=/"+routeEntity.getRouteId()+"/**"));
//                    }};
//                    Optional.ofNullable(routeEntity.getPredicates()).ifPresent(n -> predicateDefinitionList.addAll(routeEntity.getPredicates()));
//                    setPredicates(predicateDefinitionList);
                    Optional.ofNullable(routeEntity.getPredicates()).ifPresent(n -> setPredicates(routeEntity.getPredicates()));
                    Optional.ofNullable(routeEntity.getUri()).ifPresent(n -> setUri(URI.create(routeEntity.getUri())));
                    Optional.ofNullable(routeEntity.getOrder()).ifPresent(n -> setOrder(routeEntity.getOrder()));
                    Optional.ofNullable(routeEntity.getMetadata()).ifPresent(n -> setMetadata(routeEntity.getMetadata()));

                }});
            });
        log.info("Route Info: \n"+objectMapper.writeValueAsString(routes));

        return Flux.fromIterable(routes.values());
    }


    /**
     * 라우트 갱신
     * */
    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(routeDefinition -> {
            //라우트 bean refresh
            publisher.publishEvent(new RefreshRoutesEvent(this));
            return Mono.empty();
        });
    }


    /**
     * 라우트 삭제
     * */
    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> {
            //라우트 bean refresh
            publisher.publishEvent(new RefreshRoutesEvent(this));
            return Mono.empty();
        });
    }

    public void restartApplication(){
        routeChangeDisposalbe.dispose();
        log.info("Dispose route event stream!:"+routeChangeDisposalbe.isDisposed());
        restartEndpoint.restart();
    }
}
