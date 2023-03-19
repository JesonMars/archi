package org.metalohe.archi.gateway.starter.service;

import org.metalohe.archi.gateway.event.RefreshRoutesEvent;
import org.metalohe.archi.gateway.filter.FilterDefinition;
import org.metalohe.archi.gateway.filter.GlobalFilter;
import org.metalohe.archi.gateway.filter.factory.GatewayFilterFactory;
import org.metalohe.archi.gateway.handler.predicate.PredicateDefinition;
import org.metalohe.archi.gateway.route.RouteDefinition;
import org.metalohe.archi.gateway.route.RouteDefinitionLocator;
import org.metalohe.archi.gateway.route.RouteDefinitionWriter;
import org.metalohe.archi.gateway.route.builder.RouteLocatorBuilder;
import org.metalohe.archi.gateway.starter.dao.mapper.IGateWayMapper;
import org.metalohe.archi.gateway.starter.dao.model.TGateWayModel;
import lombok.extern.apachecommons.CommonsLog;
import org.metalohe.archi.gateway.starter.util.SpringUtils;
import org.metalohe.archi.gateway.util.GsonUtils;
import org.metalohe.archi.gateway.util.StrHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@CommonsLog
public class RouterService implements ApplicationEventPublisherAware {

    @Value("${server.port:8001}")
    private Integer serverPort;


    @Value("${cus.router.uri}")
    private String routeUri;

    private RouteDefinitionLocator routeDefinitionLocator;
    private List<GlobalFilter> globalFilters;
    private List<GatewayFilterFactory> GatewayFilters;
    private RouteDefinitionWriter routeDefinitionWriter;
    private ApplicationEventPublisher publisher;

    public RouterService(RouteDefinitionLocator routeDefinitionLocator, List<GlobalFilter> globalFilters,
                         List<GatewayFilterFactory> GatewayFilters, RouteDefinitionWriter routeDefinitionWriter) {
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.globalFilters = globalFilters;
        this.GatewayFilters = GatewayFilters;
        this.routeDefinitionWriter = routeDefinitionWriter;
    }

    @Resource
    FilterService filterService;

    @Resource
    IGateWayMapper gateWayMapper;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    public RouteLocatorBuilder.Builder getRouterBuilder(RouteLocatorBuilder builder) {
        log.info("本机启动uri:" + routeUri);
        RouteLocatorBuilder.Builder route = builder.routes()
                .route("/hello", r -> r.path("/hello")
                        .filters(x -> x.analysisHttpRequestBody()
                                .fastFinish()
                                .modifyResponseBody(String.class, String.class, MediaType.ALL_VALUE,
                                        ((exchange, s) -> {
                                            return Mono.just("success");
                                        })
                                )
                        ).uri(routeUri)
                );
        return route;
    }

    public String initRouter() {
        List<TGateWayModel> tGateWayModels = gateWayMapper.selectCusRouter();

        List<RouteDefinition> routeDefinitions = tGateWayModels.stream().flatMap(model -> {
            RouteDefinition definition = getRouterByGatewayModel(model);
            this.routeDefinitionWriter.save(Mono.just(definition)).subscribe();
            return Stream.of(definition);
        }).collect(Collectors.toList());

        String result = GsonUtils.toJson(routeDefinitions);
        this.refresh();
        return result;
    }

    public Boolean refresh() {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return true;
    }

    public RouteDefinition getRouterByGatewayModel(TGateWayModel model) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(model.getRouterCode());
        String routerUri = model.getRouterUri();
        if (StrHelper.isEmptyOrNull(routerUri)) {
            routerUri = routeUri;
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(routerUri).build().toUri();
        // URI uri = UriComponentsBuilder.fromHttpUrl("http://baidu.com").build().toUri();
        definition.setUri(uri);
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");

        Map<String, String> predicateParams = new HashMap<>(8);
        predicateParams.put("pattern", model.getPath());
        predicate.setArgs(predicateParams);

        //查询获取到所有filter
        List<FilterDefinition> byRouterCode = filterService.getByRouterCode(model.getRouterCode());
        //定义Filter

        definition.setFilters(byRouterCode);
        definition.setPredicates(Arrays.asList(predicate));
        return definition;
    }

    public Boolean deleteOne(String code) {
        Mono<Map<String, RouteDefinition>> routeDefs = this.routeDefinitionLocator.getRouteDefinitions()
                .collectMap(RouteDefinition::getId);
        Mono<Boolean> booleanMono = routeDefs.filter(x -> Objects.nonNull(x.get(code))).hasElement();
        Boolean aBoolean = booleanMono.flux().blockFirst();
        if (aBoolean) {
            this.routeDefinitionWriter.delete(Mono.just(code));
            this.refresh();
            return true;
        }
        return false;
    }

    public List<RouteDefinition> refreshRouters() {

        List<TGateWayModel> tGateWayModels = gateWayMapper.selectCusRouter();
        List<RouteDefinition> routeDefinitions = tGateWayModels.stream().flatMap(model -> {
            RouteDefinition definition = getRouterByGatewayModel(model);
            this.deleteOne(model.getRouterCode());

            this.routeDefinitionWriter.save(Mono.just(definition)).subscribe();
            return Stream.of(definition);
        }).collect(Collectors.toList());

        this.refresh();
        return routeDefinitions;
    }


}