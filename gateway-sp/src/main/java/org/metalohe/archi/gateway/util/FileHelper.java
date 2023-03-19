package org.metalohe.archi.gateway.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {
    public FileHelper() {
    }

    public static List<File> getFiles(String dir, String... extension) {
        File f = new File(dir);
        if (!f.isDirectory()) {
            return null;
        } else {
            List<File> fileList = new ArrayList();
            getFiles(f, fileList, extension);
            return fileList;
        }
    }

    private static void getFiles(File f, List<File> fileList, String... extension) {
        File[] files = f.listFiles();
        if (files != null) {
            for(int i = 0; i < files.length; ++i) {
                if (files[i].isDirectory()) {
                    getFiles(files[i], fileList, extension);
                } else if (files[i].isFile()) {
                    String fileName = files[i].getName().toLowerCase();
                    boolean isAdd = false;
                    if (extension != null) {
                        String[] arr$ = extension;
                        int len$ = extension.length;

                        for(int i$ = 0; i$ < len$; ++i$) {
                            String ext = arr$[i$];
                            if (fileName.lastIndexOf(ext) == fileName.length() - ext.length()) {
                                isAdd = true;
                                break;
                            }
                        }
                    }

                    if (isAdd) {
                        fileList.add(files[i]);
                    }
                }
            }

        }
    }
}