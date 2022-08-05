package org.example.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.changestream.OperationType;
import org.example.entity.GatewayRoute;
import org.example.service.RouteService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

@Slf4j
@Component
public class CachedRouteDefinition implements RouteDefinitionRepository, ApplicationEventPublisherAware {

    private Map<String, RouteDefinition> routes = Collections.synchronizedMap(new LinkedHashMap<>());

    private ApplicationEventPublisher publisher;

    private ObjectMapper objectMapper;
    private RouteService routeService;

    public CachedRouteDefinition(RouteService routeService, ObjectMapper objectMapper) {
        this.routeService = routeService;
        this.objectMapper = objectMapper;
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
        //change event 핸들러 정의
        Flux<ChangeStreamEvent<GatewayRoute>> routeChangeEvent = routeService.routeChangeStreamListner()
                .doOnNext(changeStreamEvent -> {
                    if(changeStreamEvent.getOperationType() == OperationType.DELETE){
                        delete(changeStreamEvent.getRaw().getDocumentKey().getObjectId("_id").getValue().toHexString());
                    }else{
                        update(changeStreamEvent.getBody());
                    }
                });
        //이벤트 스트리밍 종료시 재시도
        routeChangeEvent
                .doAfterTerminate(() -> {
                    log.info("Resume route change stream!");
                    routeChangeEvent.subscribe();
                }).subscribe();


        //db에서 route 가져와서 캐싱
        routeService.getRoutes().toStream()
            .filter(routeEntity -> BooleanUtils.isTrue(routeEntity.getEnableYn()))
            .forEach(routeEntity -> updateCachedRoute(routeEntity));
        log.info("Cached route Info: \n"+objectMapper.writeValueAsString(routes));

        return Flux.fromIterable(routes.values());
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap(routeDefinition -> {
            //라우트 bean refresh
            publisher.publishEvent(new RefreshRoutesEvent(this));
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> {
            //라우트 bean refresh
            publisher.publishEvent(new RefreshRoutesEvent(this));
            return Mono.empty();
        });
    }


    public Mono<Map> getRoutes() {
        return Mono.just(this.routes);
    }



    public void updateCachedRoute(GatewayRoute routeEntity) {
        try {
            routes.put(routeEntity.get_id().toHexString(),new RouteDefinition(){{
                setId(routeEntity.getRouteId());
                Optional.ofNullable(routeEntity.getFilters()).ifPresent(n -> setFilters(routeEntity.getFilters()));
                Optional.ofNullable(routeEntity.getPredicates()).ifPresent(n -> setPredicates(routeEntity.getPredicates()));
                Optional.ofNullable(routeEntity.getUri()).ifPresent(n -> setUri(URI.create(routeEntity.getUri())));
                Optional.ofNullable(routeEntity.getOrder()).ifPresent(n -> setOrder(routeEntity.getOrder()));
                Optional.ofNullable(routeEntity.getMetadata()).ifPresent(n -> setMetadata(routeEntity.getMetadata()));
            }});
        }catch (Exception ex){
            log.error("fail to update cached route. _id: {} . message: {} ",routeEntity.get_id(),ex.getMessage());
            return;
        }
    }



    /**
     * 라우트 업데이트
     * */
    @SneakyThrows
    public void update(GatewayRoute routeEntity) {
        if(BooleanUtils.isTrue(routeEntity.getEnableYn())){
            //inmemory 라우트 정보 저장
            updateCachedRoute(routeEntity);
            //라우트 bean refresh
            publisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("Cached route changed: {}", objectMapper.writeValueAsString(routeEntity));
        }else{
            delete(Mono.just(routeEntity.get_id().toHexString()));
        }
    }

    /**
     * 라우트 삭제
     * */
    @SneakyThrows
    public void delete(String id) {
        if(routes.containsKey(id)){
            //inmemory 라우트 정보 삭제
            routes.remove(id);
            //라우트 bean refresh
            publisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("Cached route deleted: {}", id);
        }else{
            log.error("Cached route not found: {}", id);
        }
    }



    /**
     * 화이트 리스트 체크
     * @param exchange
     * */
    public boolean isWhiteListURL(ServerWebExchange exchange){
        boolean pathResult = false;
        boolean methodResult = false;

        try {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            Map<String, Object> metadata = route.getMetadata();

            String reqPath = exchange.getRequest().getURI().getPath();

            //라우트 정보에 있는 whitelist를 가져온다.
            List<Map> whitelistList;
            if(metadata.get("whitelist")!=null && metadata.get("whitelist") instanceof List) {
                whitelistList = (List<Map>) metadata.get("whitelist");
            }else{
                return false;
            }
            for(Map whitelist:whitelistList){
                pathResult = false;
                methodResult = false;
                //url pattern 체크
                String pattern = whitelist.get("pattern").toString();
                if(pattern.split("/")[pattern.split("/").length-1].contains("*")){
                    if(reqPath.startsWith(pattern.replace("*",""))) {
                        pathResult = true;
                    }
                }else if(pattern.matches(reqPath)){
                    pathResult = true;
                }
                //request method 체크
                if(pathResult) {
                    if(whitelist.get("method") instanceof List && ((List)whitelist.get("method")).size()>0) {
                        //method 체크
                        for (String method : (List<String>) whitelist.get("method")) {
                            if (exchange.getRequest().getMethodValue().equalsIgnoreCase(method)) {
                                methodResult = true;
                                break;
                            }
                        }
                        if (methodResult) {
                            break;
                        }
                    }else{
                        //method 정보가 없으면 통과
                        methodResult=true;
                        break;
                    }
                }
            }
        }catch (Exception e){
            log.error("Failed whitelist check. {}. {}",e.getMessage(), Arrays.stream(e.getStackTrace()).findFirst().get());
            return false;
        }

        if(pathResult && methodResult){
            log.info("Authorization has been passed. URL is included in the whitelist.");
            return true;
        }else{
            return false;
        }
    }
}
