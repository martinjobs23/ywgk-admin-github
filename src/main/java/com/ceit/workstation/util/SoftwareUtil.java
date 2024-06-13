package com.ceit.desktop.util;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SoftwareUtil {

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
        String savePath = SoftwareUtil.GetUploadPath(request);
        if(subDir!=null && subDir.length()>0)
        {
            savePath = Paths.get(savePath, subDir).toString();
            
            File f1 = new File(savePath);
            if (!f1.exists())
                f1.mkdirs();
        }

        String fileName = "";
        String saveName = "";
        StringBuilder saveFileName = new StringBuilder();

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
                fileName = getFileName(header).replace(".exe","");
                saveFileName.append(fileName).append(UUID.randomUUID().toString()).append(".exe");
                fileName = saveFileName.toString();
                saveName =  Paths.get(savePath, saveFileName.toString()).toString();
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
                        fileName = getFileName(header).replace(".exe","");
                        saveFileName.append(fileName).append(UUID.randomUUID().toString()).append(".exe");
                        fileName = saveFileName.toString();
                        saveName =  Paths.get(savePath, saveFileName.toString()).toString();
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


    public static String GetUploadPath(HttpServletRequest request) {
        String path = System.getProperty("upload.path");
        if (path == null)
        {
            path = request.getServletContext().getRealPath("/");
            if(path.endsWith(File.separator)) {
                path = path.substring(0, path.length()-1);
            }

            //上2级目录
            int index = path.lastIndexOf(File.separator );
            index = path.lastIndexOf(File.separator, index-1 );
            path = path.substring(0, index +1);
            path = path + "upload";
        }
        else {
            System.clearProperty("upload.path"); // 使用完清除，否则，如果先设置了，后又注释掉，tomcat不重启这个值还在
        }
        File f1 = new File(path);
        if (!f1.exists()) {
            f1.mkdirs();
        }
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
            BigInteger bigInt = new BigInteger(1, md5Bytes);// 1代表绝对值
            return bigInt.toString(16);// 转换为16进制
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
