package ipron.cloud.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class ApiDocsApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ApiDocsApplication.class);
        application.run(args);
    }

    @Bean
    public RestartEndpoint restartEndpoint(){
        return new RestartEndpoint();
    }
}

