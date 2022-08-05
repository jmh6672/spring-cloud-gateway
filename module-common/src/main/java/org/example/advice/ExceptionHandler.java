package org.example.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.response.ErrorResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;


@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(-2)
public class ExceptionHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;
    private final ExceptionAdvice exceptionAdvice;

    @SneakyThrows
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        Object returnValue = null;

        Throwable cuase = throwable.getCause() == null ? throwable : throwable.getCause();
        Class cuaseClass = cuase.getClass();

        Class exceptionAdviceClass = exceptionAdvice.getClass();
        for (Method method:exceptionAdviceClass.getDeclaredMethods()) {
            for (Class type : method.getParameterTypes()) {
                if (cuaseClass.isAssignableFrom(type)) {
                    try {
                        returnValue = method.invoke(exceptionAdvice, type.cast(cuase));
                    } catch (Exception e){
                        returnValue = exceptionAdvice.handleHttpException(e);
                    }
                    throwable = new Throwable(cuase);
                }
            }
        }

        //ExceptionAdvice 수행
        if(returnValue == null) {
            assert throwable instanceof Exception;
            returnValue = exceptionAdvice.handleHttpException(throwable);
        }
        exceptionLog(throwable);

        ResponseEntity<ErrorResult> responseEntity = (ResponseEntity<ErrorResult>) returnValue;

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(responseEntity.getStatusCode());
        // header set
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // set response body
        ErrorResult result = responseEntity.getBody();
        byte[] bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);

        log.info("Response -> {}", response.getStatusCode());
        exchange.getRequest();
        return response.writeWith(Flux.just(response.bufferFactory().wrap(bytes)));
    }

    /**
     * Exception 로그
     * @param throwable : Exception
     * */
    public void exceptionLog(Throwable throwable){
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if(stackTraceElements==null || stackTraceElements.length < 1){
            stackTraceElements = throwable.getCause().getStackTrace();
        }
        String methodName = stackTraceElements[0].getMethodName();
        String fileName = stackTraceElements[0].getFileName();
        int lineNumber = stackTraceElements[0].getLineNumber();

        //Exception 발생 위치와 message만 로깅
        log.error("({}:{}.{}) Message: {}", fileName, lineNumber, methodName, throwable.getMessage());

        // 상세로그 보기 위하여 추가
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        log.debug(sw.toString());
    }
}
