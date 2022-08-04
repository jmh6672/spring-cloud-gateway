package ipron.cloud.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@SpringBootApplication
public class ApiGatewayApplication {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;


    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}