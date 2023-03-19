//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.metalohe.archi.gateway.starter.util;


import org.metalohe.archi.gateway.util.FileHelper;
import org.metalohe.archi.gateway.util.StrHelper;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.JarFile;

class ClassScanerUtil {
    private ClassLoader cl = Thread.currentThread().getContextClassLoader();
    private File tempUnpackFolder;

    ClassScanerUtil() {
    }

    public Set<Class<?>> scan(String... basePakages) throws URISyntaxException, IOException, ClassNotFoundException {
        Set<Class<?>> classes = new LinkedHashSet();
        if (basePakages != null && basePakages.length > 0 && !StrHelper.isEmptyOrNull(basePakages[0])) {
            String[] arr$ = basePakages;
            int len$ = basePakages.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String pack = arr$[i$];
                classes.addAll(this.scanByPakage(pack));
            }
        } else {
            classes.addAll(this.scanByURLClassLoader());
            classes.addAll(this.scanByJarPath(ClassHelperUtil.getJarPath(ClassScanerUtil.class)));
        }

        this.deleteTempUnpackFolder();
        return classes;
    }

    public Set<Class<?>> scanByPakage(String pack) throws URISyntaxException, MalformedURLException, FileNotFoundException, ClassNotFoundException {
        Set<Class<?>> classes = new LinkedHashSet();
        String packageDirName = pack.replace('.', '/');

        try {
            Enumeration dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);

            while(dirs.hasMoreElements()) {
                URL url = (URL)dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    this.getClassFromURL(url, pack, classes);
                } else if ("jar".equals(protocol)) {
                    try {
                        JarFile jar = ((JarURLConnection)url.openConnection()).getJarFile();
                        classes.addAll(ClassHelperUtil.GetClassFromJar(jar, ConstUtil.RPC_CLASS, pack));
                    } catch (IOException var9) {
                        var9.printStackTrace();
                    }
                }
            }
        } catch (IOException var10) {
            var10.printStackTrace();
        }

        this.deleteTempUnpackFolder();
        return classes;
    }

    public Set<Class<?>> scanByJarPath(String jarPath) throws IOException, ClassNotFoundException {
        System.out.println("jarPath:" + jarPath);
        Set<Class<?>> classes = new LinkedHashSet();
        List<File> jarFiles = FileHelper.getFiles(jarPath, new String[]{"jar"});
        if (jarFiles == null) {
            System.out.println("No find jar from path:" + jarPath);
        } else {
            Iterator i$ = jarFiles.iterator();

            while(i$.hasNext()) {
                File f = (File)i$.next();
                classes.addAll(ClassHelperUtil.GetClassFromJar(f.getPath(), ConstUtil.RPC_CLASS));
            }
        }

        return classes;
    }

    private Set<Class<?>> scanByURLClassLoader() {
        Set<Class<?>> classes = new LinkedHashSet();
        Set<URL> urlSet = new HashSet();
        URL[] urlAry = ((URLClassLoader)Thread.currentThread().getContextClassLoader()).getURLs();
        URL[] sysUrlAry = ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
        URL[] arr$ = urlAry;
        int len$ = urlAry.length;

        int i$;
        URL url;
        for(i$ = 0; i$ < len$; ++i$) {
            url = arr$[i$];
            urlSet.add(url);
        }

        arr$ = sysUrlAry;
        len$ = sysUrlAry.length;

        for(i$ = 0; i$ < len$; ++i$) {
            url = arr$[i$];
            urlSet.add(url);
        }

        Iterator i = urlSet.iterator();

        while(i.hasNext()) {
            URL url$ = (URL)i.next();
            if (!url$.getPath().equalsIgnoreCase("/")) {
                try {
                    System.out.println("scanByURLClassLoader:" + URLDecoder.decode(url$.getPath(), "utf-8"));
                    if (url$.getPath().endsWith(".jar")) {
                        classes.addAll(ClassHelperUtil.GetClassFromJar(URLDecoder.decode(url$.getPath(), "utf-8"), ConstUtil.RPC_CLASS));
                    } else {
                        this.getClassFromURL(url$, "", classes);
                    }
                } catch (Exception var9) {
                    System.out.println("scanByURLClassLoader error.[" + url$ + "]." + var9);
                }
            }
        }

        return classes;
    }

    private void getClassFromURL(URL url, String basePak, Set<Class<?>> classes) throws MalformedURLException, URISyntaxException, FileNotFoundException, IOException, ClassNotFoundException {
        if (url == null) {
            System.err.println("url is null when getClassFromURL");
        } else {
            String path = URLDecoder.decode(url.getPath(), "utf-8");
            if (path != null && !path.equalsIgnoreCase("")) {
                File f = new File(path);
                if (f.isDirectory()) {
                    List<File> files = FileHelper.getFiles(f.getAbsolutePath(), new String[]{"class"});
                    Iterator i$ = files.iterator();

                    while(i$.hasNext()) {
                        File file = (File)i$.next();
                        Class<?> c = this.getClassFromFile(file, url, basePak);
                        if (c != null) {
                            classes.add(c);
                        }
                    }
                } else if (f.getName().endsWith(".class")) {
                    Class<?> c = this.getClassFromFile(f, url, basePak);
                    if (c != null) {
                        classes.add(c);
                    }
                } else if (path.startsWith("file:") && path.endsWith("jar!/")) {
                    try {
                        String tempFile = this.get(url);
                        classes.addAll(ClassHelperUtil.GetClassFromJar(tempFile, ConstUtil.RPC_CLASS));
                    } catch (Exception var10) {
                    }
                }

            } else {
                System.err.println("path is null when getClassFromURL (url:" + url + ")");
            }
        }
    }

    private Class<?> getClassFromFile(File f, URL baseURL, String basePak) throws ClassNotFoundException, URISyntaxException, FileNotFoundException, IOException {
        if (!isSerializable(f)) {
            return null;
        } else {
            String filePath = f.getAbsolutePath();
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }

            filePath = filePath.replace(System.getProperty("file.separator"), ".");
            String dirPath = baseURL.toURI().getPath();
            if (dirPath.startsWith("/")) {
                dirPath = dirPath.substring(1);
            }

            dirPath = dirPath.replace("/", ".");
            filePath = filePath.replace(dirPath, "");
            if (filePath.endsWith(".class")) {
                filePath = filePath.substring(0, filePath.length() - ".class".length());
            }

            Class<?> c = this.cl.loadClass(basePak + filePath);
            return c;
        }
    }

    private static boolean isSerializable(File f) throws FileNotFoundException, IOException {
        if (!f.getAbsolutePath().endsWith(".class")) {
            return false;
        } else {
            boolean result = false;
            StringBuffer sb = new StringBuffer();
            FileReader fr = null;
            BufferedReader br = null;

            try {
                fr = new FileReader(f);
                br = new BufferedReader(fr);

                while(true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }

                    sb.append(line);
                    if (sb.indexOf(ConstUtil.RPC_CLASS) > -1) {
                        result = true;
                        break;
                    }
                }
            } finally {
                if (fr != null) {
                    fr.close();
                }

                if (br != null) {
                    br.close();
                }

            }

            return result;
        }
    }

    private String get(URL url) throws Exception {
        String name = URLDecoder.decode(url.getPath(), "utf-8");
        if (name.lastIndexOf("!/") != -1) {
            name = name.substring(0, name.lastIndexOf("!/"));
        }

        if (name.lastIndexOf(47) != -1) {
            name = name.substring(name.lastIndexOf(47) + 1);
        }

        File file = new File(this.getTempUnpackFolder(), name);
        if (!file.exists()) {
            this.unpack(url, file);
        }

        return file.getPath();
    }

    private File getTempUnpackFolder() {
        if (this.tempUnpackFolder == null) {
            File tempFolder = new File(System.getProperty("java.io.tmpdir"));
            this.tempUnpackFolder = this.createUnpackFolder(tempFolder);
        }

        return this.tempUnpackFolder;
    }

    private void deleteTempUnpackFolder() {
        try {
            if (this.tempUnpackFolder != null) {
                this.cleanDirectory(this.tempUnpackFolder);
                this.tempUnpackFolder.delete();
            }
        } catch (Exception var2) {
        }

    }

    private void cleanDirectory(File directory) {
        File[] files = directory.listFiles();
        File[] arr$ = files;
        int len$ = files.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            File file = arr$[i$];
            file.delete();
        }

    }

    private File createUnpackFolder(File parent) {
        int var2 = 0;

        File unpackFolder;
        do {
            if (var2++ >= 1000) {
                throw new IllegalStateException("Failed to create unpack folder in directory '" + parent + "'");
            }

            unpackFolder = new File(parent, "scan-libs-" + UUID.randomUUID());
        } while(!unpackFolder.mkdirs());

        return unpackFolder;
    }

    private void unpack(URL url, File file) throws IOException {
        JarURLConnection jarConnection = (JarURLConnection)url.openConnection();
        InputStream inputStream = jarConnection.getInputStream();
        OutputStream outputStream = new FileOutputStream(file);
        byte[] buffer = new byte['耀'];

        int bytesRead;
        while((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.flush();
        outputStream.close();
    }
}
