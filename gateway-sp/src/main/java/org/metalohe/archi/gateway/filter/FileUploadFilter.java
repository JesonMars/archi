package org.metalohe.archi.gateway.filter;

import org.metalohe.archi.gateway.support.ServerWebExchangeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.*;

/**
 * @author zhangxinxiu
 * 拦截请求，获取到请求数据，并返回
 */
public class FileUploadFilter implements GlobalFilter, Ordered {
    private static Logger logger=LoggerFactory.getLogger(FileUploadFilter.class);
    private static final List<HttpMessageReader<?>> MESSAGE_READERS = HandlerStrategies.withDefaults().messageReaders();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 快速返回,不调用业务系统
        logger.info("请求地址："+exchange.getRequest().getPath().toString());
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                        DataBufferUtils.retain(buffer);
                        return Mono.just(buffer);
                    });

                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return cachedFlux;
                        }
                    };
                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                    return cacheBody(mutatedExchange,chain);
                });
//                .flatMap(x->{
//            try {
//                getFile(x);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return Mono.empty();
//        });
//        ServerHttpResponse response = exchange.getResponse();
//        return response.writeWith(Mono.empty());
    }

    @Override
    public int getOrder() {
        return 10000;
    }

    private Mono<Void> cacheBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        final HttpHeaders headers = exchange.getRequest().getHeaders();
        if (headers.getContentLength() == 0) {
            return chain.filter(exchange);
        }
        if(!headers.getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA)){
            return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(x->{
                byte[] bytes = getFile(x);
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_POST_FILE_BYTE,bytes);
                return chain.filter(exchange);
            });
        }
        final ResolvableType resolvableType;
        resolvableType = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
        return MESSAGE_READERS.stream().filter(reader -> reader.canRead(resolvableType, exchange.getRequest().getHeaders().getContentType())).findFirst()
                .orElseThrow(() -> new IllegalStateException("未发现上传的数据")).readMono(resolvableType, exchange.getRequest(), Collections.emptyMap()).flatMap(resolvedBody -> {
                    if (resolvedBody instanceof MultiValueMap) {
                        logger.info("解析到文件类型为MultiValueMap");
                        Set set = ((MultiValueMap) resolvedBody).keySet();
                        Map param=new HashMap();
                        Iterator iterator = set.iterator();
                        logger.info("启动循环解析");
                        while (iterator.hasNext()){
                            Object next = iterator.next();
                            final Part partInfo = (Part) ((MultiValueMap) resolvedBody).getFirst(next+"");
                            if (partInfo instanceof FilePart) {
                                logger.info("文件解析开始");
                                FilePart filePart = (FilePart) partInfo;
                                String filePath=String.format("%s%s%s",getPath(),File.separator,filePart.filename());
                                logger.info("解析获取到文件路径："+filePath);
                                File file=new File(filePath);
                                filePart.transferTo(file);
                                byte[] bytes=getBytesByFile(file);
                                logger.info("解析获取到文件："+filePath);
                                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_POST_FILE_BYTE,bytes);
                                file.delete();
                            }
                        }
//                gatewayContext.setRequestBody(resolvedBody);
                    } else {
                        logger.info("请求地址：{},请求参数:{}",exchange.getRequest().getPath().toString(),resolvedBody);
                        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_POST_BODY_RAW,resolvedBody);
                    }
                    return chain.filter(exchange);
        });
    }

    public static String getPath() {
        String path = System.getProperty("user.dir");
//                Thread.currentThread().getContextClassLoader().getResource("").getPath();
        File filePath=new File(path);
        if(filePath.isDirectory()){
            return path;
        }
        return path;
    }

    public static byte[] getFile(DataBuffer x) {
        InputStream inputStream = x.asInputStream();
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        byte[] bb=new byte[2048];
        int ch= 0;
        try {
            ch = inputStream.read(bb);
            while(ch!=-1){
                byteArrayOutputStream.write(bb,0,ch);
                ch=inputStream.read(bb);
            }
            byte[] result=byteArrayOutputStream.toByteArray();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //将文件转换成Byte数组
    public static byte[] getBytesByFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            byte[] data = bos.toByteArray();
            bos.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void readBin2File(byte[] byteArray, String targetPath) {
        InputStream in = new ByteArrayInputStream(byteArray);
        File file = new File(targetPath);
        String path = targetPath.substring(0, targetPath.lastIndexOf("/"));
        if (!file.exists()) {
            new File(path).mkdir();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = in.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}