package org.metalohe.archi.gateway.starter;


import lombok.extern.apachecommons.CommonsLog;
import org.metalohe.archi.gateway.route.RouteLocator;
import org.metalohe.archi.gateway.route.builder.RouteLocatorBuilder;
import org.metalohe.archi.gateway.starter.service.RouterService;
import org.metalohe.archi.gateway.starter.service.ThirdJarService;
import org.metalohe.archi.gateway.starter.util.SpringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import javax.annotation.Resource;

@SpringBootApplication
@SpringBootConfiguration
//@EnableAutoConfiguration
//@RestController
@EnableScheduling
@CommonsLog
//@Import(value = GatewayControllerEndpoint.class)
public class GateWayApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(GateWayApplication.class, args);
        SpringUtils.setApplicationContext(run);
    }

    @Resource
    RouterService routerService;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("启动网关请求");
        RouteLocatorBuilder.Builder routerBuilder = routerService.getRouterBuilder(builder);
        return routerBuilder.build();
    }

    @Bean
    public RouterFunction<ServerResponse> initJar(ConfigurableApplicationContext context){
        RouterFunction<ServerResponse> route = RouterFunctions.route(
                RequestPredicates.path("/initjar"),
                (ex) -> {
                    ThirdJarService bean = context.getBean(ThirdJarService.class);
                    bean.initThirdJar();
                    return ServerResponse.ok().body(BodyInserters.fromObject("s"));
                });
        return route;
    }


}
