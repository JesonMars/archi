package org.metalohe.archi.gateway.filter.factory;

import org.metalohe.archi.gateway.filter.GatewayFilter;
import org.metalohe.archi.gateway.filter.FastFinishFilter;
import org.springframework.http.codec.ServerCodecConfigurer;

/**
 * @author zhangxinxiu
 * 拦截请求，获取到请求数据，并返回
 */
public class FastFinishGatewayFilterFactory extends AbstractGatewayFilterFactory<FastFinishGatewayFilterFactory.Config>  {

    private final ServerCodecConfigurer codecConfigurer;

    public FastFinishGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
        super(FastFinishGatewayFilterFactory.Config.class);
        this.codecConfigurer = codecConfigurer;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> new FastFinishFilter().filter(exchange,chain));
    }

    public static class Config {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}