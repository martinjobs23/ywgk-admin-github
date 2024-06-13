package com.ceit.admin.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import com.ceit.admin.common.utils.SM3Utils;
import com.ceit.admin.model.UserInfo;
import com.ceit.admin.service.PasswordService;
import com.ceit.admin.service.SmsService;
import com.ceit.admin.service.UserService;
import com.ceit.admin.service.WxService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.json.JSON;
import com.ceit.response.Result;

@Controller("/login")
public class LoginController {

    @Autowired
    private SimpleJDBC simpleJDBC;

    @Autowired
    private JSON json;

    @Autowired
    PasswordService passwordService;

    @Autowired
    SmsService smsService;

    @Autowired
    WxService wxService;

   //账号密码方式登录
    @RequestMapping("/passwordLogin")
    public Result passwordLogin(Map<String, Object> reqBody, HttpServletRequest request) {
        return passwordService.passwordLogin(reqBody,request);
    }

    //判断是否首次登录
    @RequestMapping("/isFirstLogin")
    public Result isFirstLogin(Map<String, Object> reqBody){
        return passwordService.isFirstLogin(reqBody);
    }

    //初次登录设置新密码
    @RequestMapping("/setPassword")
    public Result setPassword(Map<String, Object> reqBody){
 
        String type =(String) reqBody.get("type");
        int id = Integer.parseInt(UserService.getCurrentUserId());
 
        //需要验证原密码
        if(type == "2" || type.equals("2")){
        	
            String oldpassword_encypted =(String) reqBody.get("oldpassword");
            if(oldpassword_encypted ==null || oldpassword_encypted.isEmpty())
           	 	return new Result(401,"原密码不能为空！","error");
            
            //SM2解密
            String oldpassword = PasswordService.sm2Decrypt(oldpassword_encypted);
            if(oldpassword ==null || oldpassword.isEmpty())
            	 return new Result(401,"原密码格式错误或解密失败！","error");
            
            //验证原密码是否正确
            String oldpassword_sm3 = "{SM3}" + SM3Utils.encrypt(oldpassword);
            
            String sql = "select count(*) from sys_user_password where user_id=? and password=?";
            int match = simpleJDBC.selectForOneInt(sql, id, oldpassword_sm3);
            if(match<=0)
            	return new Result(401,"原密码验证失败，不能修改密码！","error");
         }

        //新密码
        String newpassword_encypted =(String) reqBody.get("password");
        if(newpassword_encypted ==null || newpassword_encypted.isEmpty())
       	 	return new Result(401,"新密码不能为空！","error");
 
        //SM2解密
        String newpassword = PasswordService.sm2Decrypt(newpassword_encypted);
        if(newpassword ==null || newpassword.isEmpty())
        	 return new Result(401,"新密码格式错误或解密失败！","error");
        
         //验证密码复杂度
         Result result = passwordService.complexityCheck(id, newpassword);
         if (!result.getData().equals("success")) {
        	 return new Result(401,"新密码不符合密码复杂度要求！","error");
         }
 
         // 密码是否存在
         String sql = "select count(*) from sys_user_password where user_id=?";
         int count= simpleJDBC.selectForOneInt(sql, id) ;

         if (count == 0) {
             sql = "insert into sys_user_password "
                     + "(password, user_id, change_time, locked) "
                     + "VALUES(?, ?, NOW(), 0);";
         } else {
             sql = "update sys_user_password set password=?, change_time = NOW() where user_id=?";
         }

         String newpassword_sm3 = "{SM3}" + SM3Utils.encrypt(newpassword);
         int ret = simpleJDBC.update(sql,newpassword_sm3, id);
         if (ret > 0) {
             result.setData("success");
             result.setMsg("设置密码成功");
 
             return result;
         }

         result.setData("error");
         result.setMsg("写入数据库失败");

         return result;

    }

    @RequestMapping("/checkLogCount")
    /**
     * 判断日志条数
     */
    public Result checkLogCount(){
        String sql_querytotal = "SELECT COUNT(*) FROM sercuityAudit";
        String sql_alert = "SELECT config_value FROM sys_config WHERE config_item = \"log.alertcount\" ";
        String sql = "SELECT config_value FROM sys_config WHERE config_item = \"log.maxcount\"";

        int querytotal = simpleJDBC.selectForOneInt(sql_querytotal);
        int alert = simpleJDBC.selectForOneInt(sql_alert);
        int over = simpleJDBC.selectForOneInt(sql);

        if(querytotal > over){
            return new Result(200,"success","请提醒管理员：审计记录存储容量已达到上限！");
        }else if(querytotal > alert){
            return new Result(200,"success","请提醒管理员：审计记录存储容量已接近上限！");
        }else{
            return new Result(200,"success",null);
        }
    }

    //发送短信验证码
    @RequestMapping("/sendCode")
    public Result sendCode(Map<String, Object> reqBody, HttpServletRequest request) {
        Result result = smsService.sendCode(reqBody, request);
        return result;
    }

    //验证码登录
    @RequestMapping("/smsLogin")
    public Result smsLogin(Map<String, Object> reqBody, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return smsService.smsLogin(reqBody, request);
    }

    //微信登录
    @RequestMapping("/wxLogin")
    public Result wxLogin(HttpServletRequest request, HttpServletResponse response){
        return wxService.wxLogin(request);
    }


    @RequestMapping("/logout")
    public String logout(Map<String, Object> reqBody,HttpServletRequest request) {
        HttpSession session = request.getSession();
        /*
        session.removeAttribute("userInfo");
        session.removeAttribute("userid");
        session.removeAttribute("username");
        */
        Object object = null;

        try {
            object = session.getAttribute("userInfo");
        }
        catch(Exception error)
        {

        }
        UserInfo userInfo = (UserInfo) object;
        if(userInfo!=null)
        {
            String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
            String ip = PasswordService.getClientIp(request);
            String content = "用户"+userInfo.account+"已退出";
            String type = "用户退出";
            LocalDateTime currentDateTime = LocalDateTime.now();
            String time = currentDateTime.toString();
            simpleJDBC.update(audit,userInfo.name,userInfo.account,ip,content,type,time,"低");
        }



        session.invalidate();

        return "{}";
    }

    @RequestMapping("/userInfo")
    public Result userInfo(Map<String, Object> reqBody, HttpServletRequest request) {
    	
		String sql ="select config_value from sys_config where config_item =?";
		SimpleJDBC jdbc = SimpleJDBC.getInstance();
		String errorMsg = jdbc.selectForOneString(sql, "session.errmsg");
		if(errorMsg==null || errorMsg.isEmpty())
			errorMsg ="获取用户信息失败，超时或并发限制，会话已失效，请重新登录";
		
    	HttpSession session = request.getSession();
    	if(session==null)
    		return new Result(errorMsg, 100);   
    	
    	UserInfo userInfo =null;
        
        try {
        	 userInfo = (UserInfo) session.getAttribute("userInfo");
        } 
        catch(Exception error)
        {
        	//IllegalStateException : getAttribute: 会话已失效
        	return new Result(errorMsg, 100);   
        }
        
        if(userInfo==null)
        {
            return new Result(errorMsg, 100);   
        }
        
        String tmp = "{\"userId\":" + userInfo.id
                + ",\"username\":\""+ userInfo.account
                + "\", \"avatar\":\"https://i.gtimg.cn/club/item/face/img/2/15922_100.gif\"}";
        
        return new Result("ok", 200,  tmp);
    }

}
