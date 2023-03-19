package org.metalohe.archi.gateway.filter.factory;

import org.metalohe.archi.gateway.filter.GatewayContextFilter;
import org.metalohe.archi.gateway.filter.GatewayFilter;
import org.metalohe.archi.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.server.ServerWebExchange;

/**
 * 获取请求内容，将请求内容封装到
 * @see ServerWebExchange#getAttributes() 中
 * @see ServerWebExchangeUtils#GATEWAY_POST_BODY_FORM 封装form-data提交的数据
 * @see ServerWebExchangeUtils#GATEWAY_POST_BODY_RAW 封装raw类型提交的数据
 * @author zhangxinxiu
 */
public class GatewayContextGatewayFilterFactory  extends AbstractGatewayFilterFactory<GatewayContextGatewayFilterFactory.Config> {

  private final ServerCodecConfigurer codecConfigurer;

  public GatewayContextGatewayFilterFactory(ServerCodecConfigurer codecConfigurer) {
    super(GatewayContextGatewayFilterFactory.Config.class);
    this.codecConfigurer = codecConfigurer;
  }
  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> new GatewayContextFilter().filter(exchange,chain);
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