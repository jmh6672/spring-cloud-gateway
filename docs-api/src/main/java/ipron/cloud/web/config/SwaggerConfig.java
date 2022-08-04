package ipron.cloud.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import ipron.cloud.web.entity.GatewayRoute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springdoc.core.*;
import org.springdoc.webflux.api.MultipleOpenApiWebFluxResource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;

import java.util.List;
import java.util.Optional;


@Slf4j
@Component
public class SwaggerConfig implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (applicationContext == null) {
            return bean;
        }
        if (!(bean instanceof ReactiveMongoTemplate)) {
            return bean;
        }

        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        ReactiveMongoTemplate mongoTemplate = (ReactiveMongoTemplate) bean;

        mongoTemplate.findAll(GatewayRoute.class).toStream()
                .filter(routeEntity -> (BooleanUtils.isTrue(routeEntity.getEnableYn()) && !routeEntity.getRouteId().contains("docs")))
                /**
                 * 라우트 조건에 따라 swagger 그룹을 분류 한다.
                 * */
                .forEach(routeEntity -> {
                    //Path 별 그룹 분류
                    routeEntity.getPredicates().forEach(predicateDefinition -> {
                        if(predicateDefinition.getName().equals("Path")){
                            predicateDefinition.getArgs().values().forEach(pattern -> {
                                String groupName = pattern.replace("/*","");
                                groupName = groupName.replace("*","");
                                groupName = groupName.replaceFirst("/","");

                                GroupedOpenApi group = GroupedOpenApi.builder()
                                        .pathsToMatch(pattern)
                                        .group(groupName)
                                        .build();
                                log.info("Swagger Group name = " + groupName);
                                beanFactory.registerSingleton(groupName, group);
                            });
                        }
                    });
                });


        for (String openApiBeanName : beanFactory.getBeanNamesForType(OpenAPIService.class)) {
            beanFactory.getBeanDefinition(openApiBeanName).setScope("prototype");
        }
        for (String openApiBeanName : beanFactory.getBeanNamesForType(OpenAPI.class)) {
            beanFactory.getBeanDefinition(openApiBeanName).setScope("prototype");
        }

        return bean;
    }


    @Bean
    public MultipleOpenApiWebFluxResource multipleOpenApiResource(
            List<GroupedOpenApi> groupedOpenApis,
            ObjectFactory<OpenAPIService> defaultOpenAPIBuilder,
            AbstractRequestService requestBuilder,
            GenericResponseService responseBuilder,
            OperationService operationParser,
            RequestMappingInfoHandlerMapping requestMappingHandlerMapping,
            SpringDocConfigProperties springDocConfigProperties,
            Optional<ActuatorProvider> actuatorProvider) {

        return new MultipleOpenApiWebFluxResource(
                groupedOpenApis,
                defaultOpenAPIBuilder,
                requestBuilder,
                responseBuilder,
                operationParser,
                requestMappingHandlerMapping,
                springDocConfigProperties,
                actuatorProvider);
    }
}
