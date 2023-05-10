package com.csulib.utils;


import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MyFileUtils {
    // 删除某个文件夹
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归删除子文件夹
                    deleteFolder(file);
                } else {
                    // 删除文件
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    public static String readFile(String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        byte[] buffer = new byte[10];
        StringBuilder sb = new StringBuilder();
        while (fis.read(buffer) != -1) {
            sb.append(new String(buffer));
            buffer = new byte[10];
        }
        fis.close();
        return sb.toString();
    }
    public static void saveResult(JSONObject result, String savePath) throws IOException {
        FileWriter fileWriter = new FileWriter(savePath);
        fileWriter.write(result.toString());
        fileWriter.close();
    }
    public static JSONObject readOutputResult(String fileName){
        try{
            File file = new File(fileName);
            if(!file.exists()){
                return  new JSONObject();
            }
            return new JSONObject(readFile(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void getAllFile(File fileInput, List<File> allFileList) {
        // 获取文件列表
        File[] fileList = fileInput.listFiles();
        if(fileList == null){
            return;
        }
        for (File file : fileList) {
            if (file.isDirectory()) {
                // 递归处理文件夹
                // 如果不想统计子文件夹则可以将下一行注释掉
                getAllFile(file, allFileList);
            } else {
                // 如果是文件则将其加入到文件数组中
                if (file.getName().endsWith(".java"))
                    allFileList.add(file);
            }
        }
    }

    public static void getAllApkFile(File fileInput, List<File> allFileList) {
        // 获取文件列表
        File[] fileList = fileInput.listFiles();
        assert fileList != null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                // 递归处理文件夹
                // 如果不想统计子文件夹则可以将下一行注释掉
                getAllApkFile(file, allFileList);
            } else {
                // 如果是文件则将其加入到文件数组中
                if (file.getName().endsWith(".apk"))
                    allFileList.add(file);
            }
        }
    }
}
