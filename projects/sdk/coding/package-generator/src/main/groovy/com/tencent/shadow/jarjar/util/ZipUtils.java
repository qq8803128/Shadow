package com.tencent.shadow.jarjar.util;


import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    public static boolean merge(String tartetZipFile, List<String> sourceZipFiles) {
        boolean flag = true;
        ZipOutputStream out = null;
        List<ZipInputStream> ins = new ArrayList<ZipInputStream>();
        List<FileInputStream> fins = new ArrayList<>();
        try {
            out = new ZipOutputStream(new FileOutputStream(tartetZipFile));
            HashSet<String> names = new HashSet<String>();
            for (String sourceZipFile : sourceZipFiles) {

                ZipFile zipFile = new ZipFile(sourceZipFile, Charset.forName("GBK"));
                FileInputStream fis = new FileInputStream(sourceZipFile);
                fins.add(fis);
                ZipInputStream zipInputStream = new ZipInputStream(fis);
                ins.add(zipInputStream);
                ZipEntry ze;
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ze = enumeration.nextElement();
                    if (ze.isDirectory()) {

                    } else {
                        if (names.contains(ze.getName())) {
                            continue;
                        }

                        ZipEntry oze = new ZipEntry(ze.getName());
                        out.putNextEntry(oze);
                        if (ze.getSize() > 0) {
                            DataInputStream dis = new DataInputStream(zipFile.getInputStream(ze));
                            int len = 0;
                            byte[] bytes = new byte[1024];
                            while ((len = dis.read(bytes)) > 0) {
                                out.write(bytes, 0, len);
                            }
                            out.closeEntry();
                            out.flush();
                            close(dis);
                        }
                        names.add(oze.getName());
                    }

                }
                zipInputStream.closeEntry();
                flag = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            flag = false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                for (ZipInputStream in : ins) {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                for (FileInputStream in : fins) {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return flag;
    }

    private static  void close(DataInputStream dis){
        try{
            dis.close();
        }catch (Throwable e){
            e.printStackTrace();
        }
    }
}
