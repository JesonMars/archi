package org.metalohe.archi.gateway.filter.factory;

import org.metalohe.archi.gateway.filter.FileUploadFilter;
import org.metalohe.archi.gateway.filter.GatewayFilter;
import org.springframework.http.codec.ServerCodecConfigurer;

/**
 * @author zhangxinxiu
 * 拦截请求，获取到请求数据，并返回
 */
public class FastFileUploadGatewayFilterFactory extends AbstractGatewayFilterFactory<FastFileUploadGatewayFilterFactory.Config>  {

    private final ServerCodecConfigurer codecConfigurer;

    public FastFileUploadGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
        super(FastFileUploadGatewayFilterFactory.Config.class);
        this.codecConfigurer = codecConfigurer;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> new FileUploadFilter().filter(exchange,chain));
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