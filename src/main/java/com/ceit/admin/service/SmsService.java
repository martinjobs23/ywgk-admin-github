package com.ceit.admin.service;

import com.ceit.admin.model.UserInfo;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Component;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Component
public class SmsService {

    @Autowired
    SimpleJDBC simpleJDBC;
    @Autowired
    PasswordService passwordService;

    /**
     * @Author 苏钰玲
     * @Description:发送验证码
     * @Date 19:20 2021/5/20
     **/
    public Result sendCode(Map<String, Object> reqBody, HttpServletRequest request){
        Result result = new Result();
        result.setCode(200);
        Object object = reqBody.get("mobile");
        if (object == null){
            result.setMsg("手机号不能为空");
            result.setData("error");
            return result;
        }
        String mobile = (String) object;
        Result smsCommonCheckResult = smsCommonCheck(mobile, request);
        if (smsCommonCheckResult.getMsg() != null && !smsCommonCheckResult.getMsg().equals("")){
            return smsCommonCheckResult;
        }
        UserInfo userInfo = (UserInfo) smsCommonCheckResult.getData();
        String smsCode = "888888";  //设置成固定的
        if (smsCode.equals("0000")){ //说明没成功
            result.setMsg("验证码发送失败");
            result.setData("error");
            return result;
        }
        String sql = "INSERT INTO sys_user_sms (user_id, sms, mobile) VALUES (?,?,?)";
        simpleJDBC.update(sql, userInfo.id, smsCode, mobile);
        result.setMsg("验证码发送成功，请注意查收！");
        result.setData("success");
        return result;
    }


    /**
     * @Author 苏钰玲
     * @Description:验证码登录
     * @Date 19:21 2021/5/20
     **/
    public Result smsLogin(Map<String, Object> reqBody, HttpServletRequest request){
        Result result = new Result();
        result.setCode(200);
        String mobile = (String) reqBody.get("mobile");
        String code = (String) reqBody.get("code");
        if (mobile == null || mobile.equals("") || code == null || code.equals("")){
            result.setData("error");
            result.setMsg("短信或验证码不能为空");
            return result;
        }
        Result smsCommonCheckResult = smsCommonCheck(mobile, request);
        if (smsCommonCheckResult.getMsg() != null && !smsCommonCheckResult.getMsg().equals("")){
            return smsCommonCheckResult;
        }
        UserInfo userInfo = (UserInfo) smsCommonCheckResult.getData();
        //查找发送的验证码
        String sql = "SELECT * FROM sys_user_sms WHERE mobile = ? AND user_id = ? ORDER BY id DESC LIMIT 1"; //好像不需要两个条件 userid与mobile是一对一
        Map<String, Object> userSmsMap = simpleJDBC.selectForMap(sql, mobile, userInfo.id);
        if (userSmsMap.isEmpty()){
            result.setMsg("请先获取短信验证码");
            result.setData("error");
            return result;
        }
        Date sendTime = (Date) userSmsMap.get("send_time");
        Date nowTime = new Date();
        int minutes = (int) ((nowTime.getTime() - sendTime.getTime()) / (1000 * 60));
        if (minutes > 5) { //说明验证码超时了
            result.setData("error");
            result.setMsg("请先获取短信验证码");
            return result;
        }
        if (!userSmsMap.get("sms").toString().equals(code)) { //验证码错误
            result.setData("error");
            result.setMsg("验证码错误");
            return result;
        }
        userInfo.setOkAuthPolicy("sms");
        request.getSession().setAttribute("userInfo", userInfo);
        //检查是否所有认证方式均完成
        boolean res = userInfo.checkAuth();
        if (res == false){
            result.setCode(100);
            result.setData(userInfo.unAuthPolicy.get(0)); //将没有认证的方式路径发送给前端
        }else {
            String data = "{\"accessToken\":\""+userInfo.account+"\"}";
            result.setData(data);
        }
        return result;
    }

    public Result smsCommonCheck(String mobile, HttpServletRequest request){
        String sql = "select u.* from sys_user u where mobile = ?";
        Map<String, Object> userMap = simpleJDBC.selectForMap(sql, mobile);
        if (userMap.isEmpty()){
            return new Result("此手机号未被绑定",200, "error");
        }
        Result policyCheckResult = passwordService.policyCheck(userMap, "sms", mobile,request);
        return policyCheckResult;
    }

}
