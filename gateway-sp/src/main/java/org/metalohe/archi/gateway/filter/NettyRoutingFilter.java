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

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.metalohe.archi.gateway.filter.headers.HttpHeadersFilter;
import org.metalohe.archi.gateway.support.ServerWebExchangeUtils;
import org.metalohe.archi.gateway.support.TimeoutException;
import org.springframework.beans.factory.ObjectProvider;
import org.metalohe.archi.gateway.config.HttpClientProperties;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.net.URI;
import java.util.List;

/**
 * @author Spencer Gibb
 * @author Biju Kunjummen
 */
@Slf4j
public class NettyRoutingFilter implements GlobalFilter, Ordered {

	private final HttpClient httpClient;
	private final ObjectProvider<List<HttpHeadersFilter>> headersFilters;
	private final HttpClientProperties properties;

	public NettyRoutingFilter(HttpClient httpClient,
							  ObjectProvider<List<HttpHeadersFilter>> headersFilters,
							  HttpClientProperties properties) {
		this.httpClient = httpClient;
		this.headersFilters = headersFilters;
		this.properties = properties;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		URI requestUrl = exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

		String scheme = requestUrl.getScheme();
		if (ServerWebExchangeUtils.isAlreadyRouted(exchange) || (!"http".equals(scheme) && !"https".equals(scheme))) {
			return chain.filter(exchange);
		}
		ServerWebExchangeUtils.setAlreadyRouted(exchange);

		ServerHttpRequest request = exchange.getRequest();

		final HttpMethod method = HttpMethod.valueOf(request.getMethod().toString());
		final String url = requestUrl.toString();

		HttpHeaders filtered = HttpHeadersFilter.filterRequest(this.headersFilters.getIfAvailable(),
				exchange);

		final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
		filtered.forEach(httpHeaders::set);

		String transferEncoding = request.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING);
		boolean chunkedTransfer = "chunked".equalsIgnoreCase(transferEncoding);

		boolean preserveHost = exchange.getAttributeOrDefault(ServerWebExchangeUtils.PRESERVE_HOST_HEADER_ATTRIBUTE, false);
		Flux<HttpClientResponse> responseFlux = this.httpClient
				.chunkedTransfer(chunkedTransfer)
				.request(method)
				.uri(url)
				.send((req, nettyOutbound) -> {
					req.headers(httpHeaders);

					if (preserveHost) {
						String host = request.getHeaders().getFirst(HttpHeaders.HOST);
						req.header(HttpHeaders.HOST, host);
					}
					return nettyOutbound
							.options(NettyPipeline.SendOptions::flushOnEach)
							.send(request.getBody().map(dataBuffer ->
									((NettyDataBuffer) dataBuffer).getNativeBuffer()));
				}).responseConnection((res, connection) -> {
					ServerHttpResponse response = exchange.getResponse();
					// put headers and status so filters can modify the response
					HttpHeaders headers = new HttpHeaders();

					res.responseHeaders().forEach(entry -> headers.add(entry.getKey(), entry.getValue()));

					String contentTypeValue = headers.getFirst(HttpHeaders.CONTENT_TYPE);
					if (StringUtils.hasLength(contentTypeValue)) {
						exchange.getAttributes().put(ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR, contentTypeValue);
					}

					HttpHeaders filteredResponseHeaders = HttpHeadersFilter.filter(
							this.headersFilters.getIfAvailable(), headers, exchange, HttpHeadersFilter.Type.RESPONSE);

					response.getHeaders().putAll(filteredResponseHeaders);
					HttpStatus status = HttpStatus.resolve(res.status().code());
					if (status != null) {
						response.setStatusCode(status);
					} else if (response instanceof AbstractServerHttpResponse) {
						// https://jira.spring.io/browse/SPR-16748
						((AbstractServerHttpResponse) response).setStatusCodeValue(res.status().code());
					} else {
						throw new IllegalStateException("Unable to set status code on response: " + res.status().code() + ", " + response.getClass());
					}

					// Defer committing the response until all route filters have run
					// Put client response as ServerWebExchange attribute and write response later NettyWriteResponseFilter
					exchange.getAttributes().put(ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR, res);
					exchange.getAttributes().put(ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR, connection);

					return Mono.just(res);
				});

		if (properties.getResponseTimeout() != null) {
			responseFlux = responseFlux.timeout(properties.getResponseTimeout(),
					Mono.error(new TimeoutException("Response took longer than timeout: " +
							properties.getResponseTimeout()))).onErrorMap(TimeoutException.class, th -> new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, null, th));
		}

		return responseFlux.then(chain.filter(exchange));
	}
}
