package org.metalohe.archi.gateway.starter.component;

import org.metalohe.archi.gateway.starter.service.RouterService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

@Configuration
@EnableScheduling
@EnableAutoConfiguration
@CommonsLog
public class RouterJobs {

    @Resource
    RouterService routerService;
    /**
     * 每隔1分钟重新load下第三方jar
     */
//    @Scheduled(cron = "0 0 0/1 * * ?")
    @Scheduled(fixedRate = 6000)
    public void load1(){
        String s = routerService.initRouter();
        log.info("load router："+s);
    }
}
