package org.metalohe.archi.gateway.starter.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.metalohe.archi.gateway.starter.entity.JarEntity;
import org.metalohe.archi.gateway.util.FileUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommonsLog
public class LoadClassUtil {
    public static ClassLoader classLoader=null;
//    private static ConcurrentHashMap<String,JarEntry> jarEntryConcurrentHashMap=new ConcurrentHashMap<>();
    /**
     * 通过类型string来获取到相应的类
     * @param type
     * @param thirdJarPath
     * @return
     */
    public static Class<?> loadClass(String type,String thirdJarPath){
        if(Objects.isNull(classLoader)){
            //第一次初始化全部扫描
            loadJar();
            if(!StringUtils.isEmpty(thirdJarPath)){
                loadThirdJar(thirdJarPath);
            }
        }
        return loadClassByStr(type);
    }

    /**
     * 从jar包内部load后缀是contract的jar包
     */
    public synchronized static void loadJar(){
        if(Objects.nonNull(classLoader)) return;
        JarFile localJarFile;
        List<String> jarFiles=new ArrayList<>();
        String jarPath=FileUtils.getExecJarPath();
                //System.getProperty("java.class.path");
        try {
            localJarFile = new JarFile(new File(jarPath));
            Enumeration<JarEntry> entries = localJarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                int index = name.lastIndexOf(".");
                if(index>-1){
                    if(".jar".equals(name.substring(index)) && name.contains("contract")){
                        jarFiles.add(String.format("%s!%s",jarPath,name));
                    }
                }

            }
        } catch (IOException e) {
            log.error(e);
        }
        List<URL> libUrls = new ArrayList<>();
        libUrls.addAll(jarFiles.stream().map(x-> {
            File file=new File(x);
            try {
                URL url = file.toURI().toURL();
                return url;
            } catch (MalformedURLException e) {
                log.error(e);
            }
            return null;
        }).collect(Collectors.toList()));
        classLoader = URLClassLoader.newInstance(libUrls.toArray(new URL[libUrls.size()]), Thread.currentThread().getContextClassLoader());
//        loadCustomClass();
        log.info("loaded jar:"+libUrls);
    }

    public synchronized static void loadThirdJar(List<JarEntity> jarEntities){
        if(CollectionUtils.isEmpty(jarEntities)){
            log.info("待初始化的jar为空");
            return;
        }
        //下载jar到第三方jar库
        jarEntities.stream().forEach(x-> FileUtils.downloadFromUrl(x.getDownloadUrl(),x.getPath(),x.getName()));
        String path = jarEntities.get(0).getPath();
        log.info("begin scan "+path);
        loadThirdJar(path);
        log.info("end scan "+path);
    }

    /**
     * @param jarName
     * @param path
     * @param downloadUrl
     */
    public synchronized static void loadThirdJar(String jarName,String path,@Nullable String downloadUrl){
        File file = FileUtils.downloadFromUrl(downloadUrl, path, jarName);
        if(Objects.isNull(file)){
            return;
        }
        try {
            URL url = file.toURI().toURL();
            List<URL> urlList = new ArrayList<>();
            if(!ObjectUtils.isEmpty(classLoader)){
                List<URL> oldUrls = Arrays.asList(((URLClassLoader) classLoader).getURLs());
                urlList.addAll(oldUrls);
                boolean flag=true;
                for (URL oldUrl : oldUrls) {
                    int i = compareJar(url, oldUrl);
                    if(i>=0){
                        urlList.add(url);
                        urlList.remove(oldUrl);
                        flag=false;
                        break;
                    }else if(i==-3 || i==-1){
                        flag=false;
                    }
                }
                if(flag){
                    urlList.add(url);
                }
//                if(urlList.stream().noneMatch(x->x.getPath().equals(file.getPath()))){
//                    urlList.add(url);
//                }
            }else{
                urlList.add(url);
            }

            classLoader = URLClassLoader.newInstance(urlList.toArray(new URL[urlList.size()]), Thread.currentThread().getContextClassLoader());
            loadCustomClass(path);
            log.info("loaded third jar:"+urlList);
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * 根据路径加载路径下所有jar
     * @param path
     */
    public synchronized static void loadThirdJar(String path){
        File file=new File(path);
        if(!file.isDirectory()){
            return;
        }
        File[] listFiles = file.listFiles();
        if(ObjectUtils.isEmpty(listFiles)){
            return;
        }

        List<File> jarFileList=distinctVersion(Arrays.asList(listFiles));
        List<URL> urlList=new ArrayList<>();
        if(ObjectUtils.isEmpty(classLoader)){
            urlList = jarFileList.stream().map(x -> {
                try {
                    URL url = x.toURI().toURL();
                    return url;
                } catch (MalformedURLException e) {
                    log.info("loadThirdJar加载jar失败："+x.getPath());
                    log.error(e);
                }
                return null;
            }).collect(Collectors.toList());
        }else{
            URLClassLoader urlClassLoader=(URLClassLoader) classLoader;
            URL[] urLs = urlClassLoader.getURLs();
            List<URL> oldUrls = Arrays.asList(urLs);
            urlList.addAll(oldUrls);
            for (File x : jarFileList) {
                try {
                    URL url = x.toURI().toURL();
                    boolean flag=true;
                    for (URL oldUrl : oldUrls) {
                        int i = compareJar(url, oldUrl);
                        /*
                         * -4，两个jar的项目不同；
                         * -3,cur=compareWith的版本号，但是compareWith为release版本;
                         * -2，未获取到cur路径；
                         * -1,cur<compareWith；
                         * 0,两个相同，版本也相同；
                         * 1，cur>compareWith；
                         * 2，未获取到compareWith
                         * 3，cur=compareWith的版本号，但是cur为release版本
                         */
                        if(i>=0){
                            urlList.add(url);
                            urlList.remove(oldUrl);
                            flag=false;
                            break;
                        }else if(i==-3 || i==-1){
                            flag=false;
                        }
                    }
                    if(flag){
                        urlList.add(url);
                    }
//                    if (oldUrls.stream().noneMatch(y -> isAfterJar(y,url))) {
//                        urlList.add(url);
//                    }
                } catch (MalformedURLException e) {
                    log.info("loadThirdJar加载jar失败：" + x.getPath());
                    log.error(e);
                }
            }
//            oldUrls.addAll(addUrls);
//            urlList=oldUrls;
        }
        classLoader = URLClassLoader.newInstance(urlList.toArray(new URL[urlList.size()]), Thread.currentThread().getContextClassLoader());
        loadCustomClass(path);
        log.info("loaded third jar1:"+urlList);
    }

    /**
     * 将扫描的jar包加载到
     */
    private static void loadCustomClass(String path){
        try {
            ClassScanerUtil cs = new ClassScanerUtil();
            Set<Class<?>> classes = cs.scanByJarPath(path);
            Iterator i$ = classes.iterator();

            while(i$.hasNext()) {
                Class<?> c = (Class)i$.next();
                System.out.println("scaning " + c.getPackage().getName() + "." + c.getName());

                try {
                    // TODO: 重新实现
//                    SCFSerializable ann = c.getAnnotation(SCFSerializable.class);
//                    if (ann != null) {
//                        String name = ann.name();
//                        if (name.equals(StrHelper.EmptyString)) {
//                            name = c.getSimpleName();
//                        }
//
//                        int typeId = StrHelper.GetHashcode(name);
//                        TypeMap.setTypeMap(c,typeId);
//                        String nameV3 = c.getCanonicalName();
//                        if (nameV3.equals(StrHelper.EmptyString)) {
//                            nameV3 = c.getSimpleName();
//                        }
//
//                        int typeIdV3 = StrHelper.GetHashcode(nameV3);
//                        TypeMapV3.addObjToMaps(c, typeIdV3);
//                    }
                } catch (Exception var10) {
                    var10.printStackTrace();
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * 去重list中的版本，并获取到最终版本
     * @param files
     * @return
     * @throws MalformedURLException
     */
    private static List<File> distinctVersion(List<File> files){
        List<File> listFilesNew=new ArrayList<>();
        for (File fi : files) {
            if (listFilesNew.size() == 0) {
                listFilesNew.add(fi);
                continue;
            }
            try {
                URL url=fi.toURI().toURL();
                int flag=0;
                File shouldChange=null;
                for (File newFile : listFilesNew) {
                    URL oldUrl=newFile.toURI().toURL();
                    int i = compareJar(url, oldUrl);
                    /*
                     * -4，两个jar的项目不同；
                     * -3,cur=compareWith的版本号，但是compareWith为release版本;
                     * -2，未获取到cur路径；
                     * -1,cur<compareWith；
                     * 0,两个相同，版本也相同；
                     * 1，cur>compareWith；
                     * 2，未获取到compareWith
                     * 3，cur=compareWith的版本号，但是cur为release版本
                     */
                    if(i>=0){
                        flag=1;
                        shouldChange=newFile;
                        break;
                    }else if(i>=-3 && i<0){
                        flag=-1;
                        break;
                    }
                }
                if(flag==0){
                    listFilesNew.add(fi);
                }else if(flag==1){
                    listFilesNew.remove(shouldChange);
                    listFilesNew.add(fi);
                }
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        }
        return listFilesNew;
    }

    public static Class<?> loadClassByStr(String type){
        try {
            if(!ObjectUtils.isEmpty(classLoader)){
                return classLoader.loadClass(type);
            }
        } catch (Exception e) {
            log.info(e.getCause());
        }
        return null;
    }

    /**
     * 比较cur当前的版本是否大于待比较的版本
     * @param cur  当前版本
     * @param compareWith 待比较版本
     * @return
     * -4，两个jar的项目不同；
     * -3,cur=compareWith的版本号，但是compareWith为release版本;
     * -2，未获取到cur路径；
     * -1,cur<compareWith；
     * 0,两个相同，版本也相同；
     * 1，cur>compareWith；
     * 2，未获取到compareWith
     * 3，cur=compareWith的版本号，但是cur为release版本
     */
    private static int compareJar(URL cur,URL compareWith){
        String curPath = cur.getPath();
        String compareWithPath = compareWith.getPath();
        if(StringUtils.isEmpty(curPath)){
            return -2;
        }
        if(StringUtils.isEmpty(compareWithPath)){
            return 2;
        }
        curPath = curPath.substring(curPath.lastIndexOf("/")).replace(".jar","");
        compareWithPath = compareWithPath.substring(compareWithPath.lastIndexOf("/")).replace(".jar","");

        String curVersion = extractVersion(curPath);
        String compareVersion = extractVersion(compareWithPath);
        String curArchitectId = curPath.substring(0, curPath.lastIndexOf(curVersion));
        String compareArchitectId = compareWithPath.substring(0, compareWithPath.lastIndexOf(compareVersion));
        if(curArchitectId.equals(compareArchitectId)){
            return isAfterVersion(curPath,compareWithPath);
        }

        return -4;
    }

    /**
     * 比较cur当前的版本是否大于待比较的版本
     * @param cur  当前版本
     * @param compareWith 待比较版本
     * @return
     * -3,cur=compareWith的版本号，但是compareWith为release版本;
     * -1,cur<compareWith/两个jar的项目不同；
     * 0,两个相同，版本也相同；
     * 1，两个相同并且cur>compareWith；
     * 3，cur=compareWith的版本号，但是cur为release版本
     */
    private static int isAfterVersion(String cur,String compareWith){
        DefaultArtifactVersion current=new DefaultArtifactVersion(cur);
        DefaultArtifactVersion compare=new DefaultArtifactVersion(compareWith);
        int i = current.compareTo(compare);
        return i;
    }

    //要求jar名称必须是*-x.y.z(.|-)*的形式，x、y、z代表任意数字，且可以有多个。如abc-1.jar、abc-1.2.jar、abc-1.2.3.jar、abc-1.2.3.4.jar、abc-1.2.3-jre.jar.
    private static final String VERSION_REGEX = "-((\\d+.)+\\d)(\\.|-)";
    private static Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEX);

    /**
     * 抽取出jar包名称中的version
     *
     * @param jarName 要求jar名称必须是*-x.y.z(.|-)*的形式，x、y、z代表任意数字，且可以有多个。如abc-1.jar、abc-1.2.jar、abc-1.2.3.jar、abc-1.2.3.4.jar、abc-1.2.3-jre.jar.
     * @return
     */
    public static String extractVersion(String jarName) {
        Matcher matcher = VERSION_PATTERN.matcher(jarName);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException(String.format("非法参数[%s]，无法提取版本，请检查！", jarName));
    }
}
