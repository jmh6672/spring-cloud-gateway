package ipron.cloud.web.filter;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


import static org.springdoc.core.Constants.API_DOCS_URL;
import static org.springdoc.core.Constants.SWAGGGER_CONFIG_FILE;

@Component
@Slf4j
public class OpenAPIFilter implements WebFilter {
	@Value(API_DOCS_URL) String API_URL;

	/**
	 *  swagger의 group은 API_DOCS_URL 경로 뒤에 group명이 붙은 url로 분기된다.
	 *  그래서 게이트웨이의 라우트 옵션에 따라 분기하게 되면 predicate 조건과 맞지않아서
	 *  API_DOCS_URL로 들어온 요청에 대해서 url을 `group명 + API_DOCS_URL`로 변경해서 보내주도록 한다.
	 **/
	@SneakyThrows
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();

		String requestURI = request.getPath().value();
		log.info("Request URI:"+requestURI);

		if(requestURI.split(API_URL).length > 1 && !requestURI.contains(SWAGGGER_CONFIG_FILE)){
			String groupUrl = requestURI.split(API_URL)[1];
			if(groupUrl.lastIndexOf("/") == (groupUrl.length()-1)) groupUrl=groupUrl.substring(0,groupUrl.length()-1);
			log.info("GroupUrl:"+groupUrl+API_URL);

			exchange = exchange.mutate().request(
					exchange.getRequest().mutate().path(groupUrl+API_URL).build()
			).build();

			return chain.filter(exchange);
		}

		return chain.filter(exchange);
	}
}
