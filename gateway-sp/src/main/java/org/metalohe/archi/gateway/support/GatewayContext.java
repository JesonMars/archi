package org.metalohe.archi.gateway.support;

import org.springframework.util.MultiValueMap;

import java.util.HashMap;


public class GatewayContext {

  public static final String CACHE_GATEWAY_CONTEXT = "cacheGatewayContext";

  public String getCacheBody() {
    return cacheBody;
  }

  public void setCacheBody(String cacheBody) {
    this.cacheBody = cacheBody;
  }


  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  /**
   * cache json body
   */
  private String cacheBody;
  /**
   * cache reqeust path
   */
  private String path;

  private Object requestBody;

  public Object getRequestBody() {
    return requestBody;
  }

  public void setRequestBody(Object requestBody) {
    this.requestBody = requestBody;
  }
}