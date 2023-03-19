package org.metalohe.archi.gateway.util;

import lombok.extern.apachecommons.CommonsLog;

import java.io.File;
import java.net.URL;

/**
 * 文件操作类
 * @author zhangxinxiu
 */
@CommonsLog
public class FileUtils extends org.apache.commons.io.FileUtils {
    public static File downloadFromUrl(String url,String dir,String name) {
        try {
            File jarFile=new File(String.format("%s/%s",dir,name));
            if(jarFile.exists()){
                return jarFile;
            }

            File filePath = new File(dir);
            if (!filePath.exists()) {
                filePath.mkdir();
            }

            URL httpurl = new URL(url);
            copyURLToFile(httpurl, jarFile);
            return jarFile;
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    public static String getExecJarPath(){
        /**
         * 方法一：获取当前可执行jar包所在目录
         */
        String filePath = System.getProperty("java.class.path");
        String pathSplit = System.getProperty("path.separator");//得到当前操作系统的分隔符，windows下是";",linux下是":"

        /**
         * 若没有其他依赖，则filePath的结果应当是该可运行jar包的绝对路径，
         * 此时我们只需要经过字符串解析，便可得到jar所在目录
         */
        if(filePath.contains(pathSplit)){
            filePath = filePath.substring(0,filePath.indexOf(pathSplit));
        }
//        else if (filePath.endsWith(".jar")) {//截取路径中的jar包名,可执行jar包运行的结果里包含".jar"
//            filePath = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1);
//        }
        return filePath;
    }
}