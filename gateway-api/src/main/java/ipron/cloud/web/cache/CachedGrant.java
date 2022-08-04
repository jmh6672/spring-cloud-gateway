package ipron.cloud.web.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.changestream.OperationType;
import ipron.cloud.web.entity.AccountGrant;
import ipron.cloud.web.service.GrantService;
import ipron.cloud.web.validator.scope.AuthLevel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


@Slf4j
@Component
public class CachedGrant implements InitializingBean {
    private Map<String, Map<String, AccountGrant>> grantsByTenent = Collections.synchronizedMap(new LinkedHashMap<>());

    private ObjectMapper objectMapper;

    private GrantService grantService;

    public CachedGrant(GrantService grantService, ObjectMapper objectMapper) {
        this.grantService = grantService;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public void afterPropertiesSet() {
        //change event 핸들러 정의
        Flux<ChangeStreamEvent<AccountGrant>> grantChangeEvent = grantService.grantChangeStreamListner()
                .doOnNext(changeStreamEvent -> {
                    if(changeStreamEvent.getOperationType() == OperationType.DELETE){;
                        delete(changeStreamEvent.getRaw().getDocumentKey().getObjectId("_id").getValue().toHexString());
                    }else{
                        update(changeStreamEvent.getBody());
                        try {
                            log.info("Grant changed : "+ objectMapper.writeValueAsString(changeStreamEvent.getBody()));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });
        //이벤트 스트리밍 종료시 재시도
        grantChangeEvent
                .doAfterTerminate(() -> {
                    log.info("Resume grant change stream!");
                    grantChangeEvent.subscribe();
                }).subscribe();

        //db에서 grant가져와서 캐싱
        grantService.getGrants().toStream()
                .filter(grantEntity -> !StringUtils.isEmpty(grantEntity.getPattern()))
                .forEach(grantEntity -> update(grantEntity));

        log.info("Cached grant Info: \n"+objectMapper.writeValueAsString(grantsByTenent));
    }

    public Mono<Map> getGrantsByTenent() {
        return Mono.just(this.grantsByTenent);
    }


    @SneakyThrows
    public boolean update(AccountGrant grantEntity){
        try {
            if(grantsByTenent.containsKey(grantEntity.getTntId().toHexString())){
                grantsByTenent.get(grantEntity.getTntId().toHexString())
                        .put(grantEntity.getPattern(),grantEntity);
            }else{
                grantsByTenent.put(grantEntity.getTntId().toHexString(),
                        new HashMap<>(){{
                            put(grantEntity.getPattern(),grantEntity);
                }});
            }
            return true;
        }catch (Exception ex){
            log.error("Fail to update cached grant. _id: {}  || message: {} ",grantEntity.get_id(),ex.getMessage());
            return false;
        }
    }


    @SneakyThrows
    public void delete(String id){
        boolean delete = false;
        try {
            for (String tntId : grantsByTenent.keySet()) {
                for (String pattern : grantsByTenent.get(tntId).keySet()) {
                    if (grantsByTenent.get(tntId).get(pattern).get_id().toHexString().equals(id)) {
                        grantsByTenent.get(tntId).remove(pattern);
                        delete = true;
                        log.info("Cached grant deleted: {}", id);
                    }
                }
            }
            if(!delete) {
                log.error("Cached grant not found: {}",id);
            }
        }catch (Exception e){
            log.error("Failed cached grant delete: {}",e.getMessage());
        }
    }


    /**
     * 인가 정보 확인
     * @param exchange
     * @param tntId
     * @param authLevel
     * */
    public boolean isGrantValid(ServerWebExchange exchange, String tntId, String authLevel){
        boolean pathResult = false;
        boolean methodResult = false;
        boolean roleResult = false;

        try {
            //superadmin 의 경우 통과
            if(authLevel.equals(AuthLevel.SUPER_ADMIN.getValue())){
                return true;
            }

            String reqPath = exchange.getRequest().getURI().getPath();

            Map<String, AccountGrant> grants;
            if(grantsByTenent.containsKey(tntId)) {
                grants = grantsByTenent.get(tntId);
            }else{
                return false;
            }
            for(String pattern : grants.keySet()){
                pathResult = false;
                methodResult = false;
                roleResult = false;
                //URL pattern 체크
                if(pattern.split("/")[pattern.split("/").length-1].contains("*")){
                    //TODO: 현재 테넌트의 데이터만 조회
                    if(reqPath.startsWith(pattern.replace("*",""))) {
                        pathResult = true;
                    }
                }else if(reqPath.matches(pattern)){
                    pathResult = true;
                }

                if(pathResult){
                    AccountGrant grantEntity = grants.get(pattern);
                    //method 체크
                    if(grantEntity.getMethod() != null && grantEntity.getMethod().size() > 0) {
                        for (String method:grantEntity.getMethod()) {
                            if(exchange.getRequest().getMethodValue().equalsIgnoreCase(method)){
                                methodResult = true;
                                break;
                            }
                        }
                    }else{
                        methodResult = true;
                    }

                    //grantRole 체크
                    if(methodResult && grantEntity.getGrantRole()!=null && grantEntity.getGrantRole().size()>0) {
                        for (String role:grantEntity.getGrantRole()) {
                            if(authLevel.equalsIgnoreCase(role)){
                                roleResult = true;
                                break;
                            }
                        }
                        if(roleResult){
                            break;
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error("Failed grant check. {}",e.getMessage());
            return false;
        }

        if(pathResult && methodResult && roleResult){
            return true;
        }else{
            return false;
        }
    }
}
