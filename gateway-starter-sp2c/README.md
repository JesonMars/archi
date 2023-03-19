# 网关gateway
## 主要功能点:
- 在开源的spring-cloud-gateway基础上扩展了uri的转发，在mysql中配置转发的url，可以动态配置url的转发；
- 通过在t_gateway表中配置path前缀可以实现反向代理转发uri；
- 在t_gateway表中配置转发后，可以在t_gateway_filter表中配置请求的过滤器，通过router_code字段进行关联。
****
## 表结构说明
- t_gateway表，通过path路径进行转发

| | router_code | path | segment | router_uri | crud_code | is_delete |
|:---|:------------|:-----|:--------|:-----------|:----------|:----------|
| | 路由的编码 | 要拦截的url的路径前缀 | 要匹配的url后缀 | 路由路径转发的http地址 | 数据库操作code，扩展项 | 是否删除，0否，1是 |
| 示例1:全路径匹配,请求被转发到http://127.0.0.1:8009/test/**,如：http://127.0.0.1:8009/test/a,http://127.0.0.1:8009/test/a/b... | gateway_test | /test/** | null | http://127.0.0.1:8009/ | null | 0 |
| 示例2:通过segment固定匹配,http://127.0.0.1:8009/test1/*,如：http://127.0.0.1:8009/test1/a,http://127.0.0.1:8009/test1/b | gateway_test1 | /test1/{segment} | null | http://127.0.0.1:8010/ | null | 0 |

- t_gateway_filter表，t_gateway扩展表，配置过滤器

| | router_code | filter_name | filter_args | is_delete |
|:---|:---------|:------------|:------------|:----------|
| | 路由的编码 | 过滤器名称 | 过滤器参 | 是否删除，0否，1是 |
| 示例1:对请求添加header | gateway_test | AddRequestHeader | {"_genkey_0":"header","_genkey_1":"addHeader"} | 0 |