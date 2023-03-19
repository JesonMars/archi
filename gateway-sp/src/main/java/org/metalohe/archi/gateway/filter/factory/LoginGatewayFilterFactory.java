package org.metalohe.archi.gateway.filter.factory;

import org.metalohe.archi.gateway.entity.ResponseEntity;
import org.metalohe.archi.gateway.filter.GatewayFilter;
import org.metalohe.archi.gateway.util.GsonUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangxinxiu
 * 拦截请求，验证是否已经登录
 */
public class LoginGatewayFilterFactory extends AbstractGatewayFilterFactory<LoginGatewayFilterFactory.Config>  {

    private final ServerCodecConfigurer codecConfigurer;

    public LoginGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
        super(LoginGatewayFilterFactory.Config.class);
        this.codecConfigurer = codecConfigurer;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            List<String> strings = headers.get(config.getKey());
            if(!CollectionUtils.isEmpty(strings)){
                return chain.filter(exchange);
            }
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity responseEntity = ResponseEntity.buildRes("20", "登录失败,未获取到：" + config.key, null);
            String result = GsonUtils.toJson(responseEntity);
            DataBuffer dataBuffer = response.bufferFactory().allocateBuffer().write(result.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(dataBuffer));
        });
    }

    public static class Config {
        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

}