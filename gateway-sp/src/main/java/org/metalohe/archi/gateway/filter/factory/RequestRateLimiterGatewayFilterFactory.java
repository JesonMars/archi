/*
 * Copyright 2013-2017 the original author or authors.
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

package org.metalohe.archi.gateway.filter.factory;

import org.metalohe.archi.gateway.filter.GatewayFilter;
import org.metalohe.archi.gateway.filter.ratelimit.KeyResolver;
import org.metalohe.archi.gateway.filter.ratelimit.RateLimiter;
import org.metalohe.archi.gateway.support.ServerWebExchangeUtils;
import org.metalohe.archi.gateway.route.Route;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * User Request Rate Limiter filter. See https://stripe.com/blog/rate-limiters and
 */
public class RequestRateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<RequestRateLimiterGatewayFilterFactory.Config> {

	public static final String KEY_RESOLVER_KEY = "keyResolver";

	private final RateLimiter defaultRateLimiter;
	private final KeyResolver defaultKeyResolver;

	public RequestRateLimiterGatewayFilterFactory(RateLimiter defaultRateLimiter,
												  KeyResolver defaultKeyResolver) {
		super(Config.class);
		this.defaultRateLimiter = defaultRateLimiter;
		this.defaultKeyResolver = defaultKeyResolver;
	}

	public KeyResolver getDefaultKeyResolver() {
		return defaultKeyResolver;
	}

	public RateLimiter getDefaultRateLimiter() {
		return defaultRateLimiter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public GatewayFilter apply(Config config) {
		KeyResolver resolver = (config.keyResolver == null) ? defaultKeyResolver : config.keyResolver;
		RateLimiter<Object> limiter = (config.rateLimiter == null) ? defaultRateLimiter : config.rateLimiter;

		return (exchange, chain) -> {
			Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

			return resolver.resolve(exchange).flatMap(key ->
					// TODO: if key is empty?
					limiter.isAllowed(route.getId(), key).flatMap(response -> {

						for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
							exchange.getResponse().getHeaders().add(header.getKey(), header.getValue());
						}

						if (response.isAllowed()) {
							return chain.filter(exchange);
						}

						exchange.getResponse().setStatusCode(config.getStatusCode());
						return exchange.getResponse().setComplete();
					}));
		};
	}

	public static class Config {
		private KeyResolver keyResolver;
		private RateLimiter rateLimiter;
		private HttpStatus statusCode = HttpStatus.TOO_MANY_REQUESTS;

		public KeyResolver getKeyResolver() {
			return keyResolver;
		}

		public Config setKeyResolver(KeyResolver keyResolver) {
			this.keyResolver = keyResolver;
			return this;
		}
		public RateLimiter getRateLimiter() {
			return rateLimiter;
		}

		public Config setRateLimiter(RateLimiter rateLimiter) {
			this.rateLimiter = rateLimiter;
			return this;
		}

		public HttpStatus getStatusCode() {
			return statusCode;
		}

		public Config setStatusCode(HttpStatus statusCode) {
			this.statusCode = statusCode;
			return this;
		}
	}

}
