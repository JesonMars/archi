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
 */

package org.metalohe.archi.gateway.route.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.metalohe.archi.gateway.filter.GatewayFilter;
import org.metalohe.archi.gateway.filter.OrderedGatewayFilter;
import org.metalohe.archi.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.metalohe.archi.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.metalohe.archi.gateway.filter.factory.rewrite.RewriteFunction;
import org.metalohe.archi.gateway.filter.ratelimit.RateLimiter;
import org.metalohe.archi.gateway.filter.factory.*;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.metalohe.archi.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.retry.Repeat;
import reactor.retry.Retry;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Applies specific filters to routes.
 */
public class GatewayFilterSpec extends UriSpec {

	private static final Log log = LogFactory.getLog(GatewayFilterSpec.class);

	public GatewayFilterSpec(Route.AsyncBuilder routeBuilder, RouteLocatorBuilder.Builder builder) {
		super(routeBuilder, builder);
	}

	/**
	 * Applies the filter to the route.
	 * @param gatewayFilter the filter to apply
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec filter(GatewayFilter gatewayFilter) {
		if (gatewayFilter instanceof Ordered) {
			this.routeBuilder.filter(gatewayFilter);
			return this;
		}
		return this.filter(gatewayFilter, 0);
	}

	/**
	 * Applies the filter to the route.
	 * @param gatewayFilter the filter to apply
	 * @param order the order to apply the filter
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec filter(GatewayFilter gatewayFilter, int order) {
		if (gatewayFilter instanceof Ordered) {
			this.routeBuilder.filter(gatewayFilter);
			log.warn("GatewayFilter already implements ordered "+gatewayFilter.getClass()
					+ "ignoring order parameter: "+order);
			return this;
		}
		this.routeBuilder.filter(new OrderedGatewayFilter(gatewayFilter, order));
		return this;
	}

	/**
	 * Applies the list of filters to the route.
	 * @param gatewayFilters the filters to apply
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec filters(GatewayFilter... gatewayFilters) {
		List<GatewayFilter> filters = transformToOrderedFilters(Stream.of(gatewayFilters));
		this.routeBuilder.filters(filters);
		return this;
	}

	public List<GatewayFilter> transformToOrderedFilters(Stream<GatewayFilter> stream) {
		return stream
				.map(filter -> {
					if (filter instanceof Ordered) {
						return filter;
					} else {
						return new OrderedGatewayFilter(filter, 0);
					}
				}).collect(Collectors.toList());
	}

	/**
	 * Applies the list of filters to the route.
	 * @param gatewayFilters the filters to apply
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec filters(Collection<GatewayFilter> gatewayFilters) {
		List<GatewayFilter> filters = transformToOrderedFilters(gatewayFilters.stream());
		this.routeBuilder.filters(filters);
		return this;
	}

	/**
	 * Adds a request header to the request before it is routed by the Gateway.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec addRequestHeader(String headerName, String headerValue) {
		return filter(getBean(AddRequestHeaderGatewayFilterFactory.class)
				.apply(c -> c.setName(headerName).setValue(headerValue)));
	}

	/**
	 * Adds a request parameter to the request before it is routed by the Gateway.
	 * @param param the parameter name
	 * @param value the parameter vaule
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec addRequestParameter(String param, String value) {
		return filter(getBean(AddRequestParameterGatewayFilterFactory.class)
				.apply(c -> c.setName(param).setValue(value)));
	}

	/**
	 * Adds a header to the response returned to the Gateway from the route.
	 * @param headerName the header name
	 * @param headerValue the header value
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec addResponseHeader(String headerName, String headerValue) {
		return filter(getBean(AddResponseHeaderGatewayFilterFactory.class)
				.apply(c -> c.setName(headerName).setValue(headerValue)));
	}

	/**
	 * Wraps the route in a Hystrix command.
	 * Depends on @{code org.springframework.cloud::spring-cloud-starter-netflix-hystrix} being on the classpath,
	 * {@see https://cloud.spring.io/spring-cloud-netflix/}
	 * @param configConsumer a {@link Consumer} which provides configuration for the Hystrix command
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec hystrix(Consumer<HystrixGatewayFilterFactory.Config> configConsumer) {
		HystrixGatewayFilterFactory factory;
		try {
			factory = getBean(HystrixGatewayFilterFactory.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			throw new NoSuchBeanDefinitionException(HystrixGatewayFilterFactory.class, "This is probably because Hystrix is missing from the classpath, which can be resolved by adding dependency on 'org.springframework.cloud:spring-cloud-starter-netflix-hystrix'");
		}
		return filter(factory.apply(this.routeBuilder.getId(), configConsumer));
	}

	/**
	 * A filter that can be used to modify the request body.
	 * This filter is BETA and may be subject to change in a future release.
	 * @param inClass the class to convert the incoming request body to
	 * @param outClass the class the Gateway will add to the request before it is routed
	 * @param rewriteFunction the {@link RewriteFunction} that transforms the request body
	 * @param <T> the original request body class
	 * @param <R> the new request body class
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	//TODO: setup custom spec
	public <T, R> GatewayFilterSpec modifyRequestBody(Class<T> inClass, Class<R> outClass, RewriteFunction<T, R> rewriteFunction) {
		return filter(getBean(ModifyRequestBodyGatewayFilterFactory.class)
				.apply(c -> c.setRewriteFunction(inClass, outClass, rewriteFunction)));
	}

	/**
	 * A filter that can be used to modify the request body.
	 * This filter is BETA and may be subject to change in a future release.
	 * @param inClass the class to convert the incoming request body to
	 * @param outClass the class the Gateway will add to the request before it is routed
	 * @param newContentType the new Content-Type header to be sent
	 * @param rewriteFunction the {@link RewriteFunction} that transforms the request body
	 * @param <T> the original request body class
	 * @param <R> the new request body class
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public <T, R> GatewayFilterSpec modifyRequestBody(Class<T> inClass, Class<R> outClass, String newContentType, RewriteFunction<T, R> rewriteFunction) {
		return filter(getBean(ModifyRequestBodyGatewayFilterFactory.class)
				.apply(c -> c.setRewriteFunction(inClass, outClass, rewriteFunction).setContentType(newContentType)));
	}
	/**
	 * A filter that can be used to modify the response body
	 * This filter is BETA and may be subject to change in a future release.
	 * @param inClass the class to conver the response body to
	 * @param outClass the class the Gateway will add to the response before it is returned to the client
	 * @param rewriteFunction the {@link RewriteFunction} that transforms the response body
	 * @param <T> the original response body class
	 * @param <R> the new response body class
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public <T, R> GatewayFilterSpec modifyResponseBody(Class<T> inClass, Class<R> outClass, RewriteFunction<T, R> rewriteFunction) {
		return filter(getBean(ModifyResponseBodyGatewayFilterFactory.class)
				.apply(c -> c.setRewriteFunction(inClass, outClass, rewriteFunction)));
	}

	/**
	 * A filter that can be used to modify the response body
	 * This filter is BETA and may be subject to change in a future release.
	 * @param inClass the class to conver the response body to
	 * @param outClass the class the Gateway will add to the response before it is returned to the client
	 * @param newContentType the new Content-Type header to be returned
	 * @param rewriteFunction the {@link RewriteFunction} that transforms the response body
	 * @param <T> the original response body class
	 * @param <R> the new response body class
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	//TODO: setup custom spec
	public <T, R> GatewayFilterSpec modifyResponseBody(Class<T> inClass, Class<R> outClass, String newContentType, RewriteFunction<T, R> rewriteFunction) {
		return filter(getBean(ModifyResponseBodyGatewayFilterFactory.class)
				.apply(c -> c.setRewriteFunction(inClass, outClass, rewriteFunction).setNewContentType(newContentType)));
	}

	/**
	 * A filter that can analysis response body
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec analysisHttpRequestBody() {
		return filter(getBean(GatewayContextGatewayFilterFactory.class)
				.apply(c->c.setName("getdateway")));
	}

	/**
	 * A filter that can analysis response body
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec fastFinish() {
		return filter(getBean(FastFinishGatewayFilterFactory.class)
				.apply(c->c.setName("fastfinish")));
	}

	/**
	 * A filter that can fast upload file
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec fastFileUpload() {
		return filter(getBean(FastFileUploadGatewayFilterFactory.class)
				.apply(c->c.setName("fastupload")));
	}
	/**
	 * A filter that can be used to add a prefix to the path of a request before it is routed by the Gateway.
	 * @param prefix the prefix to add to the path
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec prefixPath(String prefix) {
		return filter(getBean(PrefixPathGatewayFilterFactory.class)
				.apply(c -> c.setPrefix(prefix)));
	}

	/**
	 * A filter that will preserve the host header of the request on the outgoing request from the Gateway.
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec preserveHostHeader() {
		return filter(getBean(PreserveHostHeaderGatewayFilterFactory.class).apply());
	}

	/**
	 * A filter that will return a redirect response back to the client.
	 * @param status an HTTP status code, should be a {@code 300} series redirect
	 * @param url the URL to redirect to.  This URL will be set in the {@code location} header
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec redirect(int status, URI url) {
		return redirect(String.valueOf(status), url.toString());
	}

	/**
	 * A filter that will return a redirect response back to the client.
	 * @param status an HTTP status code, should be a {@code 300} series redirect
	 * @param url the URL to redirect to.  This URL will be set in the {@code location} header
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec redirect(int status, String url) {
		return redirect(String.valueOf(status), url);
	}

	/**
	 * A filter that will return a redirect response back to the client.
	 * @param status an HTTP status code, should be a {@code 300} series redirect
	 * @param url the URL to redirect to.  This URL will be set in the {@code location} header
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec redirect(String status, URI url) {
		return redirect(status, url);
	}

	/**
	 * A filter that will return a redirect response back to the client.
	 * @param status an HTTP status code, should be a {@code 300} series redirect
	 * @param url the URL to redirect to.  This URL will be set in the {@code location} header
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec redirect(String status, String url) {
		return filter(getBean(RedirectToGatewayFilterFactory.class).apply(status, url));
	}

	/**
	 * A filter that will return a redirect response back to the client.
	 * @param status an HTTP status code, should be a {@code 300} series redirect
	 * @param url the URL to redirect to.  This URL will be set in the {@code location} header
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec redirect(HttpStatus status, URL url) {
		try {
			return filter(getBean(RedirectToGatewayFilterFactory.class).apply(status, url.toURI()));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL", e);
		}
	}

	/**
	 * A filter that will remove a request header before the request is routed by the Gateway.
	 * @param headerName the name of the header to remove
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec removeRequestHeader(String headerName) {
		return filter(getBean(RemoveRequestHeaderGatewayFilterFactory.class)
				.apply(c -> c.setName(headerName)));
	}

	/**
	 * A filter that will remove a response header before the Gateway returns the response to the client.
	 * @param headerName the name of the header to remove
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec removeResponseHeader(String headerName) {
		return filter(getBean(RemoveResponseHeaderGatewayFilterFactory.class)
				.apply(c -> c.setName(headerName)));
	}

	/**
	 * A filter that will set up a request rate limiter for a route.
	 * @param configConsumer a {@link Consumer} that will return configuration for the rate limiter
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec requestRateLimiter(Consumer<RequestRateLimiterGatewayFilterFactory.Config> configConsumer) {
		return filter(getBean(RequestRateLimiterGatewayFilterFactory.class).apply(configConsumer));
	}

	/**
	 * A filter that will set up a request rate limiter for a route.
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
    public RequestRateLimiterSpec requestRateLimiter() {
		return new RequestRateLimiterSpec(getBean(RequestRateLimiterGatewayFilterFactory.class));
	}

	public class RequestRateLimiterSpec {
		private final RequestRateLimiterGatewayFilterFactory filter;

		public RequestRateLimiterSpec(RequestRateLimiterGatewayFilterFactory filter) {
			this.filter = filter;
		}

		public <C, R extends RateLimiter<C>> RequestRateLimiterSpec rateLimiter(Class<R> rateLimiterType,
                                                                                Consumer<C> configConsumer) {
			R rateLimiter = getBean(rateLimiterType);
			C config = rateLimiter.newConfig();
			configConsumer.accept(config);
			rateLimiter.getConfig().put(routeBuilder.getId(), config);
			return this;
		}

		public GatewayFilterSpec configure(Consumer<RequestRateLimiterGatewayFilterFactory.Config> configConsumer) {
			filter(this.filter.apply(configConsumer));
			return GatewayFilterSpec.this;
		}

		// useful when nothing to configure
		public GatewayFilterSpec and() {
			return configure(config -> {});
		}

	}

	/**
	 * A filter which rewrites the request path before it is routed by the Gateway
	 * @param regex a Java regular expression to match the path against
	 * @param replacement the replacement for the path
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec rewritePath(String regex, String replacement) {
		return filter(getBean(RewritePathGatewayFilterFactory.class)
				.apply(c -> c.setRegexp(regex).setReplacement(replacement)));
	}

	/**
	 * A filter that will retry failed requests.
	 * By default {@code 5xx} errors and {@code GET}s are retryable.
	 * @param retries max number of retries
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec retry(int retries) {
		return filter(getBean(RetryGatewayFilterFactory.class)
				.apply(retryConfig -> retryConfig.setRetries(retries)));
	}

	/**
	 * A filter that will retry failed requests.
	 * @param retryConsumer a {@link Consumer} which returns a {@link RetryGatewayFilterFactory.RetryConfig}
	 *                      to configure the retry functionality
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec retry(Consumer<RetryGatewayFilterFactory.RetryConfig> retryConsumer) {
		return filter(getBean(RetryGatewayFilterFactory.class).apply(retryConsumer));
	}

	/**
	 * A filter that will retry failed requests.
	 * @param repeat a {@link Repeat}
	 * @param retry a {@link Retry}
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec retry(Repeat<ServerWebExchange> repeat, Retry<ServerWebExchange> retry) {
		return filter(getBean(RetryGatewayFilterFactory.class).apply(repeat, retry));
	}

	/**
	 * A filter that adds a number of headers to the response at the reccomendation from
	 * <a href="https://blog.appcanary.com/2017/http-security-headers.html">this blog post</a>.
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	@SuppressWarnings("unchecked")
	public GatewayFilterSpec secureHeaders() {
		return filter(getBean(SecureHeadersGatewayFilterFactory.class).apply(c -> {}));
	}

	/**
	 * A filter that sets the path of the request before it is routed by the Gateway.
	 * @param template the path to set on the request, allows multiple matching segments using URI templates from
	 *                 Spring Framework
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec setPath(String template) {
		return filter(getBean(SetPathGatewayFilterFactory.class)
				.apply(c -> c.setTemplate(template)));
	}

	/**
	 * A filter that sets a header on the request before it is routed by the Gateway.
	 * @param headerName the header name
	 * @param headerValue the value of the header
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec setRequestHeader(String headerName, String headerValue) {
		return filter(getBean(SetRequestHeaderGatewayFilterFactory.class)
				.apply(c -> c.setName(headerName).setValue(headerValue)));
	}

	/**
	 * A filter that sets a header on the response before it is returned to the client by the Gateway.
	 * @param headerName the header name
	 * @param headerValue the value of the header
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec setResponseHeader(String headerName, String headerValue) {
		return filter(getBean(SetResponseHeaderGatewayFilterFactory.class)
				.apply(c -> c.setName(headerName).setValue(headerValue)));
	}

	/**
	 * A filter that sets the status on the response before it is returned to the client by the Gateway.
	 * @param status the status to set on the response
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec setStatus(int status) {
		return setStatus(String.valueOf(status));
	}

	/**
	 * A filter that sets the status on the response before it is returned to the client by the Gateway.
	 * @param status the status to set on the response
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec setStatus(HttpStatus status) {
		return setStatus(status.toString());
	}

	/**
	 * A filter that sets the status on the response before it is returned to the client by the Gateway.
	 * @param status the status to set on the response
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec setStatus(String status) {
		return filter(getBean(SetStatusGatewayFilterFactory.class)
				.apply(c -> c.setStatus(status)));
	}

	/**
	 * A filter which forces a {@code WebSession::save} operation before forwarding the call downstream. This is of
	 * particular use when using something like <a href="https://projects.spring.io/spring-session/">Spring Session</a>
	 * with a lazy data store and need to ensure the session state has been saved before making the forwarded call.
	 * If you are integrating <a href="https://projects.spring.io/spring-security/">Spring Security</a> with
	 * Spring Session, and want to ensure security details have been forwarded to the remote process, this is critical.
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	@SuppressWarnings("unchecked")
	public GatewayFilterSpec saveSession() {
		return filter(getBean(SaveSessionGatewayFilterFactory.class).apply(c -> {}));
	}

	/**
	 * Strips the prefix from the path of the request before it is routed by the Gateway.
	 * @param parts the number of parts of the path to remove
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec stripPrefix(int parts) {
		return filter(getBean(StripPrefixGatewayFilterFactory.class)
				.apply(c -> c.setParts(parts)));
	}

	/**
	 * A filter which changes the URI the request will be routed to by the Gateway by pulling it from a header on the
	 * request.
	 * @param headerName the header name containing the URI
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec requestHeaderToRequestUri(String headerName) {
		return filter(getBean(RequestHeaderToRequestUriGatewayFilterFactory.class)
				.apply(c -> c.setName(headerName)));
	}

	/**
	 * A filter which change the URI the request will be routed to by the Gateway.
	 * @param determineRequestUri a {@link Function} which takes a {@link ServerWebExchange} and returns a URI to
	 *                            route the request to
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec changeRequestUri(
			Function<ServerWebExchange, Optional<URI>> determineRequestUri) {
		return filter(
				new AbstractChangeRequestUriGatewayFilterFactory<Object>(Object.class) {
					@Override
					protected Optional<URI> determineRequestUri(
							ServerWebExchange exchange, Object config) {
						return determineRequestUri.apply(exchange);
					}
				}.apply(c -> {
				}));
	}
	

	/**
	 * A filter that sets the maximum permissible size of a Request.
	 * @param size the maximum size of a request
	 * @return a {@link GatewayFilterSpec} that can be used to apply additional filters
	 */
	public GatewayFilterSpec setRequestSize(Long size) {
		return filter(getBean(RequestSizeGatewayFilterFactory.class).apply(c -> c.setMaxSize(size)));
	}

}
