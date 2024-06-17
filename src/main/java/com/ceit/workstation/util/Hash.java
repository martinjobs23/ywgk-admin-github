package com.ceit.desktop.util;



import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

public class Hash {

    /**
     * hash
     * @param str 要加密的字符串
     * @param alg 使用的算法
     * @return
     */
    public String getHash(String str, String alg) {
        Provider provider = new BouncyCastleProvider();
        byte[] bytes = str.getBytes();
        try {
            MessageDigest digest = MessageDigest.getInstance(alg, provider);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    //MD5计算hash
    public String md5HashCode32(InputStream fis){
        try{
            //摘要算法，若想使用SHA-1或SHA-256，则传入SHA-1或SHA-256
            MessageDigest md = MessageDigest.getInstance("md5");
            //分多次将一个文件读入，对于大型文件来说，占用内存较少。
            byte[] buffer = new byte[1024];
            int length = -1;
            while((length = fis.read(buffer,0,1024)) != -1){
                md.update(buffer,0,length);
            }
            //注意！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
            //输入流关闭后，软件仓库上传软件会报错，需在md5HashCode32外部关闭输入流
//            fis.close();

            byte[] md5Bytes = md.digest();
            StringBuffer hexValue = new StringBuffer();
            for (int i = 0;i< md5Bytes.length;i++){
                int val = ((int)md5Bytes[i]) & 0xff;
                if (val<16){
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
//                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //SHA-256计算hash
    public String sha256HashCode32(InputStream fis){
        try{
            //摘要算法，若想使用SHA-1或SHA-256，则传入SHA-1或SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //分多次将一个文件读入，对于大型文件来说，占用内存较少。
            byte[] buffer = new byte[1024];
            int length = -1;
            while((length = fis.read(buffer,0,1024)) != -1){
                md.update(buffer,0,length);
            }
            //注意！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
            //输入流关闭后，软件仓库上传软件会报错，需在sha256HashCode32外部关闭输入流
//            fis.close();

            byte[] md5Bytes = md.digest();
            StringBuffer hexValue = new StringBuffer();
            for (int i = 0;i< md5Bytes.length;i++){
                int val = ((int)md5Bytes[i]) & 0xff;
                if (val<16){
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
