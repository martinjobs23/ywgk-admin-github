package com.ceit.admin.common.utils;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: FQ
 * @Date: 2020/09/09/11:06 AM
 * @Description: 手机短信连接配置
 */
public class SmsConfig {
    /**
     * url前半部分
     */
    public static final String BASE_URL = "https://openapi.miaodiyun.com/distributor/sendSMS";

    /**
     * 开发者注册后系统自动生成的账号，可在官网登录后查看
     */
    public static final String ACCOUNT_SID = "648b90f52dba94a6570e9e57d36afcbc";

    /**
     * 开发者注册后系统自动生成的TOKEN，可在官网登录后查看
     */
    public static final String AUTH_TOKEN = "9b8b289dd3c89e115cdc2335e7b9c40a";

    /**
     * 响应数据类型, JSON或XML
     */
    public static final String RESP_DATA_TYPE = "JSON";
}