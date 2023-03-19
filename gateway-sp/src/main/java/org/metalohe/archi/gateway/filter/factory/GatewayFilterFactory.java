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
import org.metalohe.archi.gateway.support.Configurable;
import org.metalohe.archi.gateway.support.NameUtils;
import org.metalohe.archi.gateway.support.ShortcutConfigurable;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.function.Consumer;

/**
 * @author Spencer Gibb
 */
@FunctionalInterface
public interface GatewayFilterFactory<C> extends ShortcutConfigurable, Configurable<C> {

	String NAME_KEY = "name";
	String VALUE_KEY = "value";

	// useful for javadsl
	default GatewayFilter apply(Consumer<C> consumer) {
		C config = newConfig();
		consumer.accept(config);
		return apply(config);
	}

	default Class<C> getConfigClass() {
		throw new UnsupportedOperationException("getConfigClass() not implemented");
	}

	@Override
	default C newConfig() {
		throw new UnsupportedOperationException("newConfig() not implemented");
	}

	GatewayFilter apply(C config);

	default String name() {
		//TODO: deal with proxys
		return NameUtils.normalizeFilterFactoryName(getClass());
	}

	@Deprecated
	default ServerHttpRequest.Builder mutate(ServerHttpRequest request) {
		return request.mutate();
	}
}
