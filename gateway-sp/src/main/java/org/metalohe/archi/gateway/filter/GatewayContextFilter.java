package org.metalohe.archi.gateway.filter;

import java.util.*;

import org.metalohe.archi.gateway.support.GatewayContext;
import org.metalohe.archi.gateway.support.ServerWebExchangeUtils;
import org.metalohe.archi.gateway.util.GsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 获取请求内容，将请求内容封装到
 *
 * @author zhangxinxiu
 * @see ServerWebExchange#getAttributes() 中
 * @see ServerWebExchangeUtils#GATEWAY_POST_BODY_FORM 封装form-data提交的数据
 * @see ServerWebExchangeUtils#GATEWAY_POST_BODY_RAW 封装raw类型提交的数据
 */
public class GatewayContextFilter implements GlobalFilter, Ordered {
    private static Logger logger = LoggerFactory.getLogger(GatewayContextFilter.class);
    /**
     * default HttpMessageReader.
     */
    private static final List<HttpMessageReader<?>> MESSAGE_READERS = HandlerStrategies.withDefaults().messageReaders();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("请求地址：" + exchange.getRequest().getPath().toString());
        final ServerHttpRequest request = exchange.getRequest();
        final String path = request.getPath().pathWithinApplication().value();
        final GatewayContext gatewayContext = new GatewayContext();
        gatewayContext.setPath(path);
        exchange.getAttributes().put(GatewayContext.CACHE_GATEWAY_CONTEXT, gatewayContext);
        final HttpHeaders headers = exchange.getRequest().getHeaders();
        if (headers.getContentLength() <= 0) {
            return chain.filter(exchange);
        }
        return DataBufferUtils
                .join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    DataBufferUtils.retain(dataBuffer);
                    final Flux<DataBuffer> cachedFlux = Flux.defer(() -> Flux.just(dataBuffer.slice(0, dataBuffer.readableByteCount())));
                    DataBufferUtils.release(dataBuffer);
                    final ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return cachedFlux;
                        }
                    };
                    final ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                    return cacheBody(mutatedExchange, chain, gatewayContext);
                });
//    return chain.filter(exchange);
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> cacheBody(ServerWebExchange exchange, GatewayFilterChain chain, GatewayContext gatewayContext) {
        final HttpHeaders headers = exchange.getRequest().getHeaders();
        if (headers.getContentLength() == 0) {
            return chain.filter(exchange);
        }
        final ResolvableType resolvableType;
        if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(headers.getContentType())) {
            resolvableType = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
        } else {
            resolvableType = ResolvableType.forClass(String.class);
        }
        return MESSAGE_READERS.stream().filter(reader -> reader.canRead(resolvableType, exchange.getRequest().getHeaders().getContentType())).findFirst()
                .orElseThrow(() -> new IllegalStateException("no suitable HttpMessageReader.")).readMono(resolvableType, exchange.getRequest(), Collections.emptyMap()).flatMap(resolvedBody -> {
                    Object printParam;
                    if (resolvedBody instanceof MultiValueMap) {
//                final Part partInfo = (Part) ((MultiValueMap) resolvedBody).getFirst("clueNo");
//                if (partInfo instanceof FormFieldPart) {
//                  gatewayContext.setRequestBody(((FormFieldPart) partInfo).value());
//                }
                        Set set = ((MultiValueMap) resolvedBody).keySet();
                        Map param = new HashMap();
                        Iterator iterator = set.iterator();
                        while (iterator.hasNext()) {
                            Object next = iterator.next();
                            final Part partInfo = (Part) ((MultiValueMap) resolvedBody).getFirst(next + "");
                            if (partInfo instanceof FormFieldPart) {
                                param.put(next, ((FormFieldPart) partInfo).value());
                            }
                        }
                        printParam = param;
                        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_POST_BODY_FORM, param);
//                gatewayContext.setRequestBody(resolvedBody);
                    } else {
                        printParam = resolvedBody;
                        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_POST_BODY_RAW, resolvedBody);
                    }
                    logger.info("请求地址：{},请求参数:{}", exchange.getRequest().getPath().toString(), GsonUtils.toJson(printParam));
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return 1001;
    }
}