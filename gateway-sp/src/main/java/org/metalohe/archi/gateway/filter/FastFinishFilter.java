package org.metalohe.archi.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author zhangxinxiu
 * 拦截请求，获取到请求数据，并返回
 */
public class FastFinishFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            // 快速返回,不调用业务系统
        return fastFinish(exchange);
    }

    @Override
    public int getOrder() {
        return 10002;
    }

    private Mono<Void> fastFinish(ServerWebExchange exchange){
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().allocateBuffer()));
    }

}