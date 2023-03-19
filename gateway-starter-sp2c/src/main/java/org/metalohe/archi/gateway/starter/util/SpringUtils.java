package org.metalohe.archi.gateway.starter.util;

import org.springframework.context.ConfigurableApplicationContext;

public class SpringUtils {
    private static ConfigurableApplicationContext applicationContext=null;
    public static ConfigurableApplicationContext getApplicationContext(){
        return applicationContext;
    }

    public static void setApplicationContext(ConfigurableApplicationContext context){
        applicationContext=context;
    }
}
