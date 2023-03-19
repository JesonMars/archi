package org.metalohe.archi.gateway.starter.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangxinxiu
 * @desc 线程池工具类
 */
public class ThreadPoolUtils {
    private static final ThreadPoolUtils instance=new ThreadPoolUtils();
    private static final ThreadPoolExecutor threadPool=new ThreadPoolExecutor(5, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private ThreadPoolUtils(){

    }
    public static ThreadPoolUtils getInstance(){
        return instance;
    }

    public ThreadPoolExecutor getThreadPool(){
        return threadPool;
    }

    public void execute(Runnable runnable){
        threadPool.execute(runnable);
    }

}
