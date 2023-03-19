package org.metalohe.archi.gateway.support.ipresolver;

import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;

/**
 * @author Andrew Fitzgerald
 */
public interface RemoteAddressResolver {

	default InetSocketAddress resolve(ServerWebExchange exchange) {
		return exchange.getRequest().getRemoteAddress();
	}
}
