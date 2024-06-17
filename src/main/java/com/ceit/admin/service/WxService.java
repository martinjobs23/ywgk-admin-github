package com.ceit.admin.service;


import com.ceit.admin.common.utils.HttpClientUtils;
import com.ceit.admin.model.UserInfo;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Component;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.json.JSON;
import com.ceit.response.Result;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

@Component
public class WxService {
    
    @Autowired
    SimpleJDBC simpleJDBC;
    @Autowired
    PasswordService passwordService;

    public Result wxLogin(HttpServletRequest request) {
        //获取code值
        String code = request.getParameter("code");
        if(code == null){
            return new Result("授权失败",200,"");
        }else {
            try {
                //获取到了code，回调没问题
                //定义地址
                String token_url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                        "appid=wx92b6693b8c01fc87&secret=d734ba63f66b3b573d7cb1cdcb958eea&code="
                        + code + "&grant_type=authorization_code";
                //发送请求 获取到请求的结果
                String token_content = HttpClientUtils.sendGetRequest(token_url, null);
                //将json字符串转成对象
                InputStream is = new ByteArrayInputStream(token_content.getBytes("UTF-8"));
                JSON json = new JSON();
                Map<String, Object> str2map = json.inputstream2Map(is);
                is.close();
                String openid = (String) str2map.get("openid");
                //根据openid查找是否有此微信账户
                String sql = "select u.id, u.account from sys_user_wx w join sys_user u where wx = ? and w.user_id = u.id";
                Map<String, Object> userMap = simpleJDBC.selectForMap(sql, openid);
                if (userMap == null) {
                    return new Result("此微信号未被绑定", 200, "error");
                }
                //3.查找用户的认证策略 --是否需要微信登录
                Result policyCheckRes = passwordService.policyCheck(userMap, "wx", openid, request);
                if (policyCheckRes.getMsg() != null && !policyCheckRes.equals("")) {
                    return policyCheckRes;
                }
                UserInfo userInfo = (UserInfo) policyCheckRes.getData();
                //获取个人信息
                /*String access_token = (String) str2map.get("access_token");
                String user_url = "https://api.weixin.qq.com/sns/userinfo?access_token="
                        + access_token +
                        "&openid=" + openid;
                String user_content = HttpClientUtils.sendGetRequest(user_url, null);*/
                userInfo.setOkAuthPolicy("pwd");
                request.getSession().setAttribute("userInfo",userInfo);
                //检查是否所有认证方式均完成
                boolean res = userInfo.checkAuth();
                if (res == false){
                    return new Result("",100,userInfo.unAuthPolicy.get(0));//将没有认证的方式路径发送给前端
                }else {
                    String data = "{\"accessToken\":\""+userInfo.account+"\"}";
                    return new Result("",200,data);//将用户信息发送给前端
                }
            } catch (Exception e) {
                e.printStackTrace();
                return new Result("微信登录错误",200,"error");
            }
        }
    }
}
