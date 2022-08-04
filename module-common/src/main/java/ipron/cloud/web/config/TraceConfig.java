package ipron.cloud.web.config;

import io.jaegertracing.internal.MDCScopeManager;
import io.opentracing.contrib.java.spring.jaeger.starter.TracerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TraceConfig {
    //로그옵션에 트레이스 정보를 MCD로 매핑할 수 있도록 하기 위함
    @Bean
    public TracerBuilderCustomizer mdcTracerBuilder(){
        return builder -> builder.withScopeManager(new MDCScopeManager.Builder().build());
    }
}
