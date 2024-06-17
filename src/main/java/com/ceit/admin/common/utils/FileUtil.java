package com.ceit.admin.common.utils;

import com.ceit.bootstrap.ConfigLoader;
import com.ceit.response.Result;
 

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.*;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileUtil {

    //标准的自定义上传（把本地文件上传至服务器中）
    //返回 上传文件名 和 存储文件物理路径
    public static Map<String,String> Upload(HttpServletRequest request)
    {
        return Upload(request,null);
    }
    //标准的自定义上传（把本地文件上传至服务器中）
    //返回 上传文件名 和 存储文件物理路径
    public static Map<String,String> Upload(HttpServletRequest request, String subDir) {

        Map<String,String> fileMap = new LinkedHashMap<>();
 
        String savePath = FileUtil.GetUploadPath(request);
        if(subDir!=null && subDir.length()>0)
        {
            savePath = Paths.get(savePath, subDir).toString();
            
            File f1 = new File(savePath);
            if (!f1.exists())
                f1.mkdirs();
        }

        String fileName = "";
        String saveName = "";
        String saveFileName = UUID.randomUUID().toString();

        try {

            // 获取上传的文件集合
            Collection<Part> parts = request.getParts();
            // 上传单个文件
            if (parts.size() == 1) {
                // Servlet3.0将multipart/form-data的POST请求封装成Part，通过Part对上传的文件进行操作。
                // Part part = parts[0];//从上传的文件集合中获取Part对象
                Part part = request.getPart("file");// 通过表单file控件(<input type="file" name="file">)的名字直接获取Part对象
                // Servlet3没有提供直接获取文件名的方法,需要从请求头中解析出来
                // 获取请求头，请求头的格式：form-data; name="file"; filename="snmp4j--api.zip"
                String header = part.getHeader("content-disposition");
                // 获取文件名
                fileName = getFileName(header);
                saveName =  Paths.get(savePath, saveFileName).toString(); 
                // 把文件写到指定路径
                part.write(saveName);
                fileMap.put(fileName, saveName);
            } else {
                // 多个部分，可能有FORMDATA + FILE，或者一次性上传多个文件
                for (Part part : parts) {// 循环处理上传的文件
                    // 获取请求头，请求头的格式：form-data; name="file"; filename="snmp4j--api.zip"
                    String header = part.getHeader("content-disposition");
                    // 获取文件名

                    if (header.contains("filename=")) {
                        fileName = getFileName(header);
                        saveName =  Paths.get(savePath, saveFileName).toString(); 
                        // 把文件写到指定路径
                        part.write(saveName);
                        fileMap.put(fileName, saveName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // System.out.println("FileController upload Error: " + e.getMessage());
            return null;
        }

        return fileMap;
    }

        /**
     * 根据请求头解析出文件名
     * 请求头的格式：火狐和google浏览器下：form-data; name="file"; filename="snmp4j--api.zip"
     * IE浏览器下：form-data; name="file"; filename="E:\snmp4j--api.zip"
     * 
     * @param header 请求头
     * @return 文件名
     */
    private static String getFileName(String header) {
        /**
         * String[] tempArr1 = header.split(";");代码执行完之后，在不同的浏览器下，tempArr1数组里面的内容稍有区别
         * 火狐或者google浏览器下：tempArr1={form-data,name="file",filename="snmp4j--api.zip"}
         * ------WebKitFormBoundarybqAUsDBGIYU0qRuH
         * Content-Disposition: form-data; name="meeting_id"
         * 
         * 68
         * ------WebKitFormBoundarybqAUsDBGIYU0qRuH
         * Content-Disposition: form-data; name="file";
         * filename="微信图片_20220222122842.jpg"
         * Content-Type: image/jpeg
         * 
         * 
         * ------WebKitFormBoundarybqAUsDBGIYU0qRuH--
         * IE浏览器下：tempArr1={form-data,name="file",filename="E:\snmp4j--api.zip"}
         */
        String[] tempArr1 = header.split(";");
        /**
         * 火狐或者google浏览器下：tempArr2={filename,"snmp4j--api.zip"}
         * Content-Disposition: form-data; name="file";
         * filename="微信图片_20220222122842.jpg"
         * IE浏览器下：tempArr2={filename,"E:\snmp4j--api.zip"}
         */
        String[] tempArr2 = tempArr1[2].split("=");
        // 获取文件名，兼容各种浏览器的写法
        String fileName = tempArr2[1].substring(tempArr2[1].lastIndexOf("\\") + 1).replaceAll("\"", "");
        return fileName;
    }

    /* 
    //标准的自定义上传（把本地文件上传至服务器中）
    public static List<String> upload(HttpServletRequest request){
        List<String> realFileNames = new ArrayList<>();
        // 使用FileItem工场类创建相应工场对象
        FileItemFactory factory = new DiskFileItemFactory();
        // 创建servlet文件上传对象并将指定工场对象传入
        ServletFileUpload fileUpload = new ServletFileUpload(factory);
        // 声明文件集合
        List<FileItem> parseRequest = null;
        try {
            // 使用servlet文件上传对象解析请求返回文件集合
            parseRequest = fileUpload.parseRequest(request);
            // 遍历文件对象集合 获取数据
            for (FileItem fileItem : parseRequest) {
                // 判断数据类型是不是普通的form表单字段
                if (!fileItem.isFormField()) {
                    // 获取上传文件的文件名
                    String fileName = fileItem.getName();
                    // 使用上传文件创建输入流
                    InputStream fileStream = fileItem.getInputStream();
                    // 使用data+文件名的方式生成保存文件的名称，避免文件重名
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String date = sdf.format(new Date());
                    String realFileName = date + fileName;
                    // 定义保存的父路径
                    File fileFatherPath = new File(System.getProperty("logUploadPath"));
                    // 创建父路径 避免路径不存在保错
                    fileFatherPath.mkdirs();
                    // 创建要保存的文件
                    File file = new File(fileFatherPath, realFileName);
                    // 创建输出流
                    OutputStream out = new FileOutputStream(file);
                    // 创建字节缓存
                    byte[] buffer = new byte[1024];
                    int len = -1;
                    // 一次读取1kb(1024byte),返回-1表明读取完毕
                    while ((len = fileStream.read(buffer)) != -1) {
                        // 一次写入1kb(1024byte)
                        out.write(buffer, 0, len);
                    }
                    // 冲刷流资源
                    out.flush();
                    // 关闭流
                    out.close();
                    fileStream.close();
                    realFileNames.add(realFileName);
                }
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
        return realFileNames;
    }

    //自动的文件拷贝（将服务器中某文件拷贝至另一目录中）
    public static boolean copyFile(String sourcePath, String newPath) {
        boolean flag = false;
        File readfile = new File(sourcePath);
        File newFile = new File(newPath);
        BufferedWriter bufferedWriter = null;
        Writer writer = null;
        FileOutputStream fileOutputStream = null;
        BufferedReader bufferedReader = null;
        try{
            fileOutputStream = new FileOutputStream(newFile, true);
            writer = new OutputStreamWriter(fileOutputStream,"UTF-8");
            bufferedWriter = new BufferedWriter(writer);
            bufferedReader = new BufferedReader(new FileReader(readfile));
            String line = null;
            while((line = bufferedReader.readLine()) != null){
                bufferedWriter.write(line);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
            flag = true;
        } catch(IOException e) {
            flag = false;
            e.printStackTrace();
        } finally {
            try {
                if(bufferedWriter != null){
                    bufferedWriter.close();
                }
                if(bufferedReader != null){
                    bufferedReader.close();
                }
                if(writer != null){
                    writer.close();
                }
                if(fileOutputStream != null){
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    //文件下载(从服务器中下载文件至本地)
    public static void download(File fileFatherPath, String file_name, HttpServletResponse response){
        File file = new File(System.getProperty("logUploadPath") + File.separator + file_name);
        // 获取下载文件的输入流
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            // 修改response
            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(file_name, "UTF-8"));
            // 写入下载文件的输出流
            File download_file = new File(fileFatherPath, file_name);
            OutputStream outputStream = new FileOutputStream(download_file);
            outputStream.write(buffer);
            System.out.println("length:" + download_file.length());
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
        }
    }
    */


    public static String GetUploadPath(HttpServletRequest request) {

        String path = ConfigLoader.getConfig("upload.path");
        if (path == null)
        {
            path = request.getServletContext().getRealPath("/");
            if(path.endsWith(File.separator))
                path = path.substring(0, path.length()-1);

            //上2级目录
            int index = path.lastIndexOf(File.separator );
            index = path.lastIndexOf(File.separator, index-1 );
            path = path.substring(0, index +1);
            path = path + "upload";
        }
        else
            System.clearProperty("upload.path"); // 使用完清除，否则，如果先设置了，后又注释掉，tomcat不重启这个值还在

        File f1 = new File(path);
        if (!f1.exists())
            f1.mkdirs();

        return path;
    }

    
    public static String GetFileMD5(String filename) {
        File f1 = new File(filename);
        return GetFileMD5(f1);
    }

    public static String GetFileMD5(File f1) {
        try {
            // 拿到一个MD5转换器,如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
            MessageDigest md = MessageDigest.getInstance("MD5");

            FileInputStream fis = new FileInputStream(f1);
            // 分多次将一个文件读入，对于大型文件而言，比较推荐这种方式，占用内存比较少。
            byte[] buffer = new byte[1024];
            int length = -1;
            while ((length = fis.read(buffer, 0, 1024)) != -1) {
                md.update(buffer, 0, length);
            }
            fis.close();

            // 转换并返回包含16个元素字节数组,返回数值范围为-128到127
            byte[] md5Bytes = md.digest();

            /* 0开头的hash 会只有31位
            BigInteger bigInt = new BigInteger(1, md5Bytes);// 1代表绝对值
            String hash = bigInt.toString(16); // 转换为16进制
            */

            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
