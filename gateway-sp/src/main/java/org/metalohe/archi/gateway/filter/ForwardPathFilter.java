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

import org.metalohe.archi.gateway.support.ServerWebExchangeUtils;
import org.metalohe.archi.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Filter to set the path in the request URI if the {@link Route} URI has the scheme
 * <code>forward</code>.
 * @author Ryan Baxter
 */
public class ForwardPathFilter implements GlobalFilter, Ordered{
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
		URI routeUri = route.getUri();
		String scheme = routeUri.getScheme();
		if (ServerWebExchangeUtils.isAlreadyRouted(exchange) || !"forward".equals(scheme)) {
			return chain.filter(exchange);
		}
		exchange = exchange.mutate().request(
				exchange.getRequest().mutate().path(routeUri.getPath()).build())
				.build();
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
