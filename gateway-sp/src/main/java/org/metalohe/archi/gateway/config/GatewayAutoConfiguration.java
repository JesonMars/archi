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

package org.metalohe.archi.gateway.config;

import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.metalohe.archi.gateway.actuate.GatewayControllerEndpoint;
import org.metalohe.archi.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.metalohe.archi.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.metalohe.archi.gateway.filter.headers.ForwardedHeadersFilter;
import org.metalohe.archi.gateway.filter.headers.HttpHeadersFilter;
import org.metalohe.archi.gateway.filter.headers.RemoveHopByHopHeadersFilter;
import org.metalohe.archi.gateway.filter.headers.XForwardedHeadersFilter;
import org.metalohe.archi.gateway.filter.ratelimit.KeyResolver;
import org.metalohe.archi.gateway.filter.ratelimit.PrincipalNameKeyResolver;
import org.metalohe.archi.gateway.filter.ratelimit.RateLimiter;
import org.metalohe.archi.gateway.filter.*;
import org.metalohe.archi.gateway.filter.factory.*;
import org.metalohe.archi.gateway.handler.FilteringWebHandler;
import org.metalohe.archi.gateway.handler.RoutePredicateHandlerMapping;
import org.metalohe.archi.gateway.handler.predicate.*;
import org.metalohe.archi.gateway.route.*;
import org.metalohe.archi.gateway.handler.predicate.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.metalohe.archi.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.ProxyProvider;
import rx.RxReactiveStreams;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", matchIfMissing = true)
@EnableConfigurationProperties
@AutoConfigureBefore({ HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class })
@AutoConfigureAfter({ GatewayLoadBalancerClientAutoConfiguration.class,
		GatewayClassPathWarningAutoConfiguration.class })
@ConditionalOnClass(DispatcherHandler.class)
public class GatewayAutoConfiguration {

	@Configuration
	@ConditionalOnClass(HttpClient.class)
	protected static class NettyConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public HttpClient httpClient(HttpClientProperties properties) {

			// configure pool resources
			HttpClientProperties.Pool pool = properties.getPool();
			ConnectionProvider connectionProvider;
			if (pool.getType() == HttpClientProperties.Pool.PoolType.DISABLED) {
				connectionProvider = ConnectionProvider.newConnection();
			} else if (pool.getType() == HttpClientProperties.Pool.PoolType.FIXED) {
				connectionProvider = ConnectionProvider.fixed(pool.getName(),
						pool.getMaxConnections(), pool.getAcquireTimeout());
			} else {
				connectionProvider = ConnectionProvider.elastic(pool.getName());
			}

			HttpClient httpClient = HttpClient.create(connectionProvider)
				.tcpConfiguration(tcpClient -> {

					if (properties.getConnectTimeout() != null) {
						tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout());
					}

					// configure proxy if proxy host is set.
					HttpClientProperties.Proxy proxy = properties.getProxy();

					if (StringUtils.hasText(proxy.getHost())) {

						tcpClient = tcpClient.proxy(proxySpec -> {
							ProxyProvider.Builder builder = proxySpec
									.type(ProxyProvider.Proxy.HTTP)
									.host(proxy.getHost());

							PropertyMapper map = PropertyMapper.get();

							map.from(proxy::getPort)
									.whenNonNull()
									.to(builder::port);
							map.from(proxy::getUsername)
									.whenHasText()
									.to(builder::username);
							map.from(proxy::getPassword)
									.whenHasText()
									.to(password -> builder.password(s -> password));
							map.from(proxy::getNonProxyHostsPattern)
									.whenHasText()
									.to(builder::nonProxyHosts);
						});
					}
					return tcpClient;
				});

			HttpClientProperties.Ssl ssl = properties.getSsl();
			if (ssl.getTrustedX509CertificatesForTrustManager().length > 0
					|| ssl.isUseInsecureTrustManager()) {
				httpClient = httpClient.secure(sslContextSpec -> {
					// configure ssl
					SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

					X509Certificate[] trustedX509Certificates = ssl
							.getTrustedX509CertificatesForTrustManager();
					if (trustedX509Certificates.length > 0) {
						sslContextBuilder.trustManager(trustedX509Certificates);
					} else if (ssl.isUseInsecureTrustManager()) {
						sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
					}

					sslContextSpec.sslContext(sslContextBuilder)
							.defaultConfiguration(ssl.getDefaultConfigurationType())
							.handshakeTimeout(ssl.getHandshakeTimeout())
							.closeNotifyFlushTimeout(ssl.getCloseNotifyFlushTimeout())
							.closeNotifyReadTimeout(ssl.getCloseNotifyReadTimeout());
				});
			}

			return httpClient;
		}

		@Bean
		public HttpClientProperties httpClientProperties() {
			return new HttpClientProperties();
		}

		@Bean
		public NettyRoutingFilter routingFilter(HttpClient httpClient,
                                                ObjectProvider<List<HttpHeadersFilter>> headersFilters,
                                                HttpClientProperties properties) {
			return new NettyRoutingFilter(httpClient, headersFilters, properties);
		}

		@Bean
		public NettyWriteResponseFilter nettyWriteResponseFilter(GatewayProperties properties) {
			return new NettyWriteResponseFilter(properties.getStreamingMediaTypes());
		}

		@Bean
		public ReactorNettyWebSocketClient reactorNettyWebSocketClient(/*@Qualifier("nettyClientOptions") Consumer<? super HttpClientOptions.Builder> options*/) {
			return new ReactorNettyWebSocketClient(/*options*/); //FIXME 2.1.0
		}
	}

	//TODO: remove when not needed anymore
	// either https://jira.spring.io/browse/SPR-17291 or
	// https://github.com/spring-projects/spring-boot/issues/14520 needs to be fixed
	@Bean
	public HiddenHttpMethodFilter disabledHiddenHttpMethodFilter() {
		return new HiddenHttpMethodFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
				return chain.filter(exchange);
			}
		};
	}

	@Bean
	public RouteLocatorBuilder routeLocatorBuilder(ConfigurableApplicationContext context) {
		return new RouteLocatorBuilder(context);
	}

	@Bean
	@ConditionalOnMissingBean
	public PropertiesRouteDefinitionLocator propertiesRouteDefinitionLocator(GatewayProperties properties) {
		return new PropertiesRouteDefinitionLocator(properties);
	}

	@Bean
	@ConditionalOnMissingBean(RouteDefinitionRepository.class)
	public InMemoryRouteDefinitionRepository inMemoryRouteDefinitionRepository() {
		return new InMemoryRouteDefinitionRepository();
	}

	@Bean
	@Primary
	public RouteDefinitionLocator routeDefinitionLocator(List<RouteDefinitionLocator> routeDefinitionLocators) {
		return new CompositeRouteDefinitionLocator(Flux.fromIterable(routeDefinitionLocators));
	}

	@Bean
	public RouteLocator routeDefinitionRouteLocator(GatewayProperties properties,
                                                    List<GatewayFilterFactory> GatewayFilters,
                                                    List<RoutePredicateFactory> predicates,
                                                    RouteDefinitionLocator routeDefinitionLocator) {
		return new RouteDefinitionRouteLocator(routeDefinitionLocator, predicates, GatewayFilters, properties);
	}

	@Bean
	@Primary
	//TODO: property to disable composite?
	public RouteLocator cachedCompositeRouteLocator(List<RouteLocator> routeLocators) {
		return new CachingRouteLocator(new CompositeRouteLocator(Flux.fromIterable(routeLocators)));
	}

	@Bean
	public RouteRefreshListener routeRefreshListener(ApplicationEventPublisher publisher) {
		return new RouteRefreshListener(publisher);
	}

	@Bean
	public FilteringWebHandler filteringWebHandler(List<GlobalFilter> globalFilters) {
		return new FilteringWebHandler(globalFilters);
	}

	@Bean
	public GlobalCorsProperties globalCorsProperties() {
		return new GlobalCorsProperties();
	}
	
	@Bean
	public RoutePredicateHandlerMapping routePredicateHandlerMapping(
			FilteringWebHandler webHandler, RouteLocator routeLocator,
			GlobalCorsProperties globalCorsProperties, Environment environment) {
		return new RoutePredicateHandlerMapping(webHandler, routeLocator,
				globalCorsProperties, environment);
	}

	// ConfigurationProperty beans

	@Bean
	public GatewayProperties gatewayProperties() {
		return new GatewayProperties();
	}

	@Bean
	public SecureHeadersProperties secureHeadersProperties() {
		return new SecureHeadersProperties();
	}

	// HttpHeaderFilter beans

	@Bean
	@ConditionalOnProperty(name = "spring.cloud.gateway.forwarded.enabled", matchIfMissing = true)
	public ForwardedHeadersFilter forwardedHeadersFilter() {
		return new ForwardedHeadersFilter();
	}

	@Bean
	public RemoveHopByHopHeadersFilter removeHopByHopHeadersFilter() {
		return new RemoveHopByHopHeadersFilter();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.cloud.gateway.x-forwarded.enabled", matchIfMissing = true)
	public XForwardedHeadersFilter xForwardedHeadersFilter() {
		return new XForwardedHeadersFilter();
	}

	// GlobalFilter beans
	
	@Bean
	public AdaptCachedBodyGlobalFilter adaptCachedBodyGlobalFilter() {
		return new AdaptCachedBodyGlobalFilter();
	}

	@Bean
	public RouteToRequestUrlFilter routeToRequestUrlFilter() {
		return new RouteToRequestUrlFilter();
	}

	@Bean
	public ForwardRoutingFilter forwardRoutingFilter(ObjectProvider<DispatcherHandler> dispatcherHandler) {
		return new ForwardRoutingFilter(dispatcherHandler);
	}

	@Bean
	public ForwardPathFilter forwardPathFilter() {
		return new ForwardPathFilter();
	}

	@Bean
	public WebSocketService webSocketService() {
		return new HandshakeWebSocketService();
	}

	@Bean
	public WebsocketRoutingFilter websocketRoutingFilter(WebSocketClient webSocketClient,
														 WebSocketService webSocketService,
														 ObjectProvider<List<HttpHeadersFilter>> headersFilters) {
		return new WebsocketRoutingFilter(webSocketClient, webSocketService, headersFilters);
	}

	@Bean
	public WeightCalculatorWebFilter weightCalculatorWebFilter(Validator validator) {
		return new WeightCalculatorWebFilter(validator);
	}

	/*@Bean
	//TODO: default over netty? configurable
	public WebClientHttpRoutingFilter webClientHttpRoutingFilter() {
		//TODO: WebClient bean
		return new WebClientHttpRoutingFilter(WebClient.routes().build());
	}

	@Bean
	public WebClientWriteResponseFilter webClientWriteResponseFilter() {
		return new WebClientWriteResponseFilter();
	}*/

	// Predicate Factory beans

	@Bean
	public AfterRoutePredicateFactory afterRoutePredicateFactory() {
		return new AfterRoutePredicateFactory();
	}

	@Bean
	public BeforeRoutePredicateFactory beforeRoutePredicateFactory() {
		return new BeforeRoutePredicateFactory();
	}

	@Bean
	public BetweenRoutePredicateFactory betweenRoutePredicateFactory() {
		return new BetweenRoutePredicateFactory();
	}

	@Bean
	public CookieRoutePredicateFactory cookieRoutePredicateFactory() {
		return new CookieRoutePredicateFactory();
	}

	@Bean
	public HeaderRoutePredicateFactory headerRoutePredicateFactory() {
		return new HeaderRoutePredicateFactory();
	}

	@Bean
	public HostRoutePredicateFactory hostRoutePredicateFactory() {
		return new HostRoutePredicateFactory();
	}

	@Bean
	public MethodRoutePredicateFactory methodRoutePredicateFactory() {
		return new MethodRoutePredicateFactory();
	}

	@Bean
	public PathRoutePredicateFactory pathRoutePredicateFactory() {
		return new PathRoutePredicateFactory();
	}

	@Bean
	public QueryRoutePredicateFactory queryRoutePredicateFactory() {
		return new QueryRoutePredicateFactory();
	}

	@Bean
	public ReadBodyPredicateFactory readBodyPredicateFactory() {
		return new ReadBodyPredicateFactory();
	}

	@Bean
	public RemoteAddrRoutePredicateFactory remoteAddrRoutePredicateFactory() {
		return new RemoteAddrRoutePredicateFactory();
	}

	@Bean
	@DependsOn("weightCalculatorWebFilter")
	public WeightRoutePredicateFactory weightRoutePredicateFactory() {
		return new WeightRoutePredicateFactory();
	}

	@Bean
	public CloudFoundryRouteServiceRoutePredicateFactory cloudFoundryRouteServiceRoutePredicateFactory() {
		return new CloudFoundryRouteServiceRoutePredicateFactory();
	}

	// GatewayFilter Factory beans

	@Bean
	public AddRequestHeaderGatewayFilterFactory addRequestHeaderGatewayFilterFactory() {
		return new AddRequestHeaderGatewayFilterFactory();
	}

	@Bean
	public AddRequestParameterGatewayFilterFactory addRequestParameterGatewayFilterFactory() {
		return new AddRequestParameterGatewayFilterFactory();
	}

	@Bean
	public AddResponseHeaderGatewayFilterFactory addResponseHeaderGatewayFilterFactory() {
		return new AddResponseHeaderGatewayFilterFactory();
	}

	@Configuration
	@ConditionalOnClass({HystrixObservableCommand.class, RxReactiveStreams.class})
	protected static class HystrixConfiguration {
		@Bean
		public HystrixGatewayFilterFactory hystrixGatewayFilterFactory(ObjectProvider<DispatcherHandler> dispatcherHandler) {
			return new HystrixGatewayFilterFactory(dispatcherHandler);
		}
	}

	@Bean
	public ModifyRequestBodyGatewayFilterFactory modifyRequestBodyGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
		return new ModifyRequestBodyGatewayFilterFactory(codecConfigurer);
	}

	@Bean
	public ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
		return new ModifyResponseBodyGatewayFilterFactory(codecConfigurer);
	}

	@Bean
	public LoginGatewayFilterFactory loginGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
		return new LoginGatewayFilterFactory(codecConfigurer);
	}

	@Bean
	public FastFinishGatewayFilterFactory fastFinishGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
		return new FastFinishGatewayFilterFactory(codecConfigurer);
	}

	@Bean
	public FastFileUploadGatewayFilterFactory fastFileUploadGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
		return new FastFileUploadGatewayFilterFactory(codecConfigurer);
	}

	@Bean
	public GatewayContextGatewayFilterFactory gatewayContextGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
		return new GatewayContextGatewayFilterFactory(codecConfigurer);
	}

	@Bean
	public PrefixPathGatewayFilterFactory prefixPathGatewayFilterFactory() {
		return new PrefixPathGatewayFilterFactory();
	}

	@Bean
	public PreserveHostHeaderGatewayFilterFactory preserveHostHeaderGatewayFilterFactory() {
		return new PreserveHostHeaderGatewayFilterFactory();
	}

	@Bean
	public RedirectToGatewayFilterFactory redirectToGatewayFilterFactory() {
		return new RedirectToGatewayFilterFactory();
	}

	@Bean
	public RemoveRequestHeaderGatewayFilterFactory removeRequestHeaderGatewayFilterFactory() {
		return new RemoveRequestHeaderGatewayFilterFactory();
	}

	@Bean
	public RemoveResponseHeaderGatewayFilterFactory removeResponseHeaderGatewayFilterFactory() {
		return new RemoveResponseHeaderGatewayFilterFactory();
	}

	@Bean(name = PrincipalNameKeyResolver.BEAN_NAME)
	@ConditionalOnBean(RateLimiter.class)
	public PrincipalNameKeyResolver principalNameKeyResolver() {
		return new PrincipalNameKeyResolver();
	}

	@Bean
	@ConditionalOnBean({RateLimiter.class, KeyResolver.class})
	public RequestRateLimiterGatewayFilterFactory requestRateLimiterGatewayFilterFactory(RateLimiter rateLimiter, PrincipalNameKeyResolver resolver) {
		return new RequestRateLimiterGatewayFilterFactory(rateLimiter, resolver);
	}

	@Bean
	public RewritePathGatewayFilterFactory rewritePathGatewayFilterFactory() {
		return new RewritePathGatewayFilterFactory();
	}

	@Bean
	public RetryGatewayFilterFactory retryGatewayFilterFactory() {
		return new RetryGatewayFilterFactory();
	}

	@Bean
	public SetPathGatewayFilterFactory setPathGatewayFilterFactory() {
		return new SetPathGatewayFilterFactory();
	}

	@Bean
	public SecureHeadersGatewayFilterFactory secureHeadersGatewayFilterFactory(SecureHeadersProperties properties) {
		return new SecureHeadersGatewayFilterFactory(properties);
	}

	@Bean
	public SetRequestHeaderGatewayFilterFactory setRequestHeaderGatewayFilterFactory() {
		return new SetRequestHeaderGatewayFilterFactory();
	}

	@Bean
	public SetResponseHeaderGatewayFilterFactory setResponseHeaderGatewayFilterFactory() {
		return new SetResponseHeaderGatewayFilterFactory();
	}

	@Bean
	public SetStatusGatewayFilterFactory setStatusGatewayFilterFactory() {
		return new SetStatusGatewayFilterFactory();
	}

	@Bean
	public SaveSessionGatewayFilterFactory saveSessionGatewayFilterFactory() {
		return new SaveSessionGatewayFilterFactory();
	}

	@Bean
	public StripPrefixGatewayFilterFactory stripPrefixGatewayFilterFactory() {
		return new StripPrefixGatewayFilterFactory();
	}

	@Bean
	public RequestHeaderToRequestUriGatewayFilterFactory requestHeaderToRequestUriGatewayFilterFactory() {
		return new RequestHeaderToRequestUriGatewayFilterFactory();
	}

	@Bean
	public RequestSizeGatewayFilterFactory requestSizeGatewayFilterFactory() {
		return new RequestSizeGatewayFilterFactory();
	}

	@Configuration
	@ConditionalOnClass(Health.class)
	protected static class GatewayActuatorConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		public GatewayControllerEndpoint gatewayControllerEndpoint(RouteDefinitionLocator routeDefinitionLocator, List<GlobalFilter> globalFilters,
                                                                   List<GatewayFilterFactory> GatewayFilters, RouteDefinitionWriter routeDefinitionWriter,
                                                                   RouteLocator routeLocator) {
			return new GatewayControllerEndpoint(routeDefinitionLocator, globalFilters, GatewayFilters, routeDefinitionWriter, routeLocator);
		}
	}

}

