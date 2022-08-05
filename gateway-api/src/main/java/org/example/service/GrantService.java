package org.example.service;

import org.example.entity.AccountGrant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
@Slf4j
public class GrantService {

    private boolean grantOnChangeEvent;

    private final String COLLECTION = "accountGrant";

    private ReactiveMongoTemplate mongoTemplate;

    public GrantService(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 권한 목록 조회*/
    public Flux<AccountGrant> getGrants(){
        return mongoTemplate.findAll(AccountGrant.class, COLLECTION);
    }

    /**
     * 권한 변경 스트림 리스너
     * */
    public Flux<ChangeStreamEvent<AccountGrant>> grantChangeStreamListner(){
        if(grantOnChangeEvent) {
            log.warn("Aleady grant streaming.");
            return Flux.just();
        }

        return this.mongoTemplate.changeStream(AccountGrant.class)
                .watchCollection(COLLECTION)
                .listen()
                .doOnSubscribe(subscription -> {
                    log.info("Start grant change stream!");
                    grantOnChangeEvent=true;
                })
                .doOnTerminate(() -> {
                    log.info("Terminate grant change stream!");
                    grantOnChangeEvent=false;
                })
                .onErrorContinue(
                      throwable -> true
                      , (throwable, o) -> log.error("Error occurred at : {} with {} ", o, throwable.getLocalizedMessage())
                );
    }

}
