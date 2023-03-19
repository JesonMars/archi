//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.metalohe.archi.gateway.starter.util;


import org.metalohe.archi.gateway.util.StrHelper;

import java.io.*;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassHelperUtil {
    public ClassHelperUtil() {
    }

    public static Set<Class<?>> GetClassFromJar(String jarPath) throws IOException, ClassNotFoundException {
        JarFile jarFile = new JarFile(jarPath);
        return GetClassFromJar(jarFile, "", "");
    }

    public static Set<Class<?>> GetClassFromJar(String jarPath, String keyword) throws IOException, ClassNotFoundException {
        JarFile jarFile = new JarFile(jarPath);
        return GetClassFromJar(jarFile, keyword, "");
    }

    public static Set<Class<?>> GetClassFromJar(JarFile jarFile, String keyword, String basePakage) throws IOException {
        Boolean recursive = true;
        String packageName = basePakage;
        String packageDirName = basePakage.replace('.', '/');
        Enumeration<JarEntry> entries = jarFile.entries();
        LinkedHashSet classes = new LinkedHashSet();

        while(entries.hasMoreElements()) {
            try {
                JarEntry entry = (JarEntry)entries.nextElement();
                String name = entry.getName();
                if (name.charAt(0) == '/') {
                    name = name.substring(1);
                }

                if (name.startsWith(packageDirName)) {
                    int idx = name.lastIndexOf(47);
                    if (idx != -1) {
                        packageName = name.substring(0, idx).replace('/', '.');
                    }

                    if ((idx != -1 || recursive) && name.endsWith(".class") && !entry.isDirectory() && checkJarEntry(jarFile, entry, keyword)) {
                        String className = name.substring(packageName.length() + 1, name.length() - 6);

                        try {
                            Class<?> c = LoadClassUtil.loadClassByStr(packageName + "." + className);
                            classes.add(c);
                        } catch (NoClassDefFoundError var14) {
                            var14.printStackTrace();
                        }
                    }
                }
            } catch (Throwable var15) {
                var15.printStackTrace();
            }
        }

        return classes;
    }

    public static boolean checkJarEntry(JarFile jarFile, JarEntry entry, String keyWord) throws IOException {
        if (keyWord != null && !keyWord.equals("")) {
            InputStream input = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;

            try {
                input = jarFile.getInputStream(entry);
                isr = new InputStreamReader(input);
                reader = new BufferedReader(isr);
                StringBuffer sb = new StringBuffer();
                boolean result = false;

                while(true) {
                    if (!result) {
                        String line = reader.readLine();
                        if (line != null) {
                            sb.append(line);
                            result = sb.indexOf(keyWord) > -1;
                            continue;
                        }
                    }

                    boolean var12 = result;
                    return var12;
                }
            } finally {
                if (input != null) {
                    input.close();
                }

                if (isr != null) {
                    isr.close();
                }

                if (reader != null) {
                    reader.close();
                }

            }
        } else {
            return true;
        }
    }

    public static boolean InterfaceOf(Class<?> type, Class<?> interfaceType) {
        if (type == null) {
            return false;
        } else {
            Class<?>[] interfaces = type.getInterfaces();
            Class[] arr$ = interfaces;
            int len$ = interfaces.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                Class<?> c = arr$[i$];
                if (c.equals(interfaceType)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static Class<?> GetClassForName(String name) throws ClassNotFoundException {
        if (name.equals("boolean")) {
            return Boolean.class;
        } else if (name.equals("char")) {
            return Character.class;
        } else if (name.equals("byte")) {
            return Byte.class;
        } else if (name.equals("short")) {
            return Short.class;
        } else if (name.equals("int")) {
            return Integer.class;
        } else if (name.equals("long")) {
            return Long.class;
        } else if (name.equals("float")) {
            return Float.class;
        } else {
            return name.equals("double") ? Double.class : Class.forName(name);
        }
    }

    public static Class<?> V3GetClassForName(String name) throws ClassNotFoundException {
        if (name.equals("boolean")) {
            return Boolean.TYPE;
        } else if (name.equals("char")) {
            return Character.TYPE;
        } else if (name.equals("byte")) {
            return Byte.TYPE;
        } else if (name.equals("short")) {
            return Short.TYPE;
        } else if (name.equals("int")) {
            return Integer.TYPE;
        } else if (name.equals("long")) {
            return Long.TYPE;
        } else if (name.equals("float")) {
            return Float.TYPE;
        } else {
            return name.equals("double") ? Double.TYPE : Class.forName(name);
        }
    }

    public static String getJarPath(Class<?> type) {
        String path = type.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replaceFirst("file:", "");
        path = path.replaceAll("!/", "");
        path = path.replaceAll("\\\\", "/");
        path = path.substring(0, path.lastIndexOf("/"));
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.indexOf("window") >= 0) {
            while(path.startsWith("/")) {
                path = path.substring(1);
            }
        }

        try {
            return URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException var4) {
            Logger.getLogger(ClassHelperUtil.class.getName()).log(Level.SEVERE, (String)null, var4);
            return path;
        }
    }

    public static String getCurrJarName(Class<?> c) {
        String filePath = c.getProtectionDomain().getCodeSource().getLocation().getFile();
        filePath = filePath.replaceFirst("file:/", "");
        filePath = filePath.replaceAll("!/", "");
        filePath = filePath.replaceAll("\\\\", "/");
        filePath = filePath.substring(filePath.lastIndexOf("/") + 1);
        return filePath;
    }
}
