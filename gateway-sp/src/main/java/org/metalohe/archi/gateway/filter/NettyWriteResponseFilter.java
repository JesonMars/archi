/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.metalohe.archi.gateway.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.metalohe.archi.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import java.util.List;

/**
 * @author Spencer Gibb
 */
public class NettyWriteResponseFilter implements GlobalFilter, Ordered {

	private static final Log log = LogFactory.getLog(NettyWriteResponseFilter.class);

	public static final int WRITE_RESPONSE_FILTER_ORDER = -1;

	private final List<MediaType> streamingMediaTypes;

	public NettyWriteResponseFilter(List<MediaType> streamingMediaTypes) {
		this.streamingMediaTypes = streamingMediaTypes;
	}

	@Override
	public int getOrder() {
		return WRITE_RESPONSE_FILTER_ORDER;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// NOTICE: nothing in "pre" filter stage as CLIENT_RESPONSE_CONN_ATTR is not added
		// until the NettyRoutingFilter is run
		return chain.filter(exchange).then(Mono.defer(() -> {
			Connection connection = exchange.getAttribute(ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR);

			if (connection == null) {
				return Mono.empty();
			}
			log.trace("NettyWriteResponseFilter start");
			ServerHttpResponse response = exchange.getResponse();

			NettyDataBufferFactory factory = (NettyDataBufferFactory) response.bufferFactory();
			//TODO: what if it's not netty

			final Flux<NettyDataBuffer> body = connection.inbound().receive()
					.retain() //TODO: needed?
					.map(factory::wrap);

			MediaType contentType = null;
			try {
				contentType = response.getHeaders().getContentType();
			} catch (Exception e) {
				log.trace("invalid media type", e);
			}
			return (isStreamingMediaType(contentType) ?
					response.writeAndFlushWith(body.map(Flux::just)) : response.writeWith(body));
		}));
	}

	//TODO: use framework if possible
	//TODO: port to WebClientWriteResponseFilter
	private boolean isStreamingMediaType(@Nullable MediaType contentType) {
		return (contentType != null && this.streamingMediaTypes.stream()
						.anyMatch(contentType::isCompatibleWith));
	}

}
