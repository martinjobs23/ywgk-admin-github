package com.ceit.admin.common.interceptors;

import com.ceit.admin.model.UserInfo;
import com.ceit.admin.service.UserService;
import com.ceit.bootstrap.ConfigLoader;
import com.ceit.interceptor.HandlerInterceptor;
import com.ceit.ioc.HandlerDefinition;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * 拦截器
 */
public class AuthInterceptor implements HandlerInterceptor {

    public static int isDebugNoAuthMode = -1;
    public boolean isDebugNoAuth()
    {
        if(isDebugNoAuthMode==0)
        {
            return false;
        }
        else if(isDebugNoAuthMode==1)
        {
            return true;
        }
        else
        {
            String noauth = ConfigLoader.getConfig("debug.noauth");
            //使用完清除，否则，如果先设置了，后又注释掉，tomcat不重启这个值还在
            //System.clearProperty("debug.noauth");  
            if(noauth!=null &&  ( noauth.equals("1") || noauth.equals("true") ))
            {
                isDebugNoAuthMode =1;
                return true;
            }
            else
            {
                isDebugNoAuthMode =0;
                return false;   
            }
        }
    }
    
    private void printResult(HttpServletResponse response, Result result)
    {
        try {
            PrintWriter out = response.getWriter();
            out.print(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerDefinition handler) {

		String sql ="select config_value from sys_config where config_item =?";
		SimpleJDBC jdbc = SimpleJDBC.getInstance();
		String errorMsg = jdbc.selectForOneString(sql, "session.errmsg");
		if(errorMsg==null || errorMsg.isEmpty())
			errorMsg ="获取用户信息失败，超时或并发限制，会话已失效，请重新登录";

        //FIXME: 开启Debug模式，会议服务无需登录可访问接口，但跳过了设置线程对象会导致部分代码有bug


        // 1.判断是否已成功登录
        HttpSession session = request.getSession();
        if(session==null)
        {
        	printResult(response, new Result(errorMsg,100,"login"));
        	return false;
        }
        
        Object object =null;
        
        try {
        	object = session.getAttribute("userInfo");
        } 
        catch(Exception error)
        {
        	//IllegalStateException : getAttribute: 会话已失效
        	printResult(response, new Result(errorMsg,100,"login"));
        	return false;
        }

        
        if (object == null) {
            
        	printResult(response, new Result(errorMsg,100,"login"));
        	session.invalidate();
            return false;
        }

        UserInfo userInfo = (UserInfo) object;
        /*
        if (userInfo.unAuthPolicy.size() != 0) {
           
        	printResult(response, new Result("用户认证信息错误，请重新登录",100,"login"));
        	session.invalidate();
            return false;
        }
  		*/
        
        //设置线程对象
        UserService.clientUserId.set(String.valueOf(userInfo.id));
        UserService.clientUserAccount.set(userInfo.account);
        UserService.clientUserName.set(userInfo.name);
        UserService.clientUserIP.set(userInfo.clientIp);

//如果是调试模式,不需要认证
        if(isDebugNoAuth())
        {
            System.out.println("!! DEBUG NO AUTH MODE !! path=" + handler.getUrl());
            return true;
        }
//        //如果是调试模式,不需要认证
//        if(isDebugNoAuth())
//        {
//            System.out.println("!! DEBUG NO AUTH MODE !! path=" + handler.getUrl());
//            return true;
//        }

        // 2.查看是否有权限
        // 获取请求方法
        String servletPath = handler.getUrl();
        //System.out.println("preHandle servletPath:" + servletPath);
        sql = "select path from sys_api where path = ? and id in (select api_id from sys_role_api where role_id in ("
                + "select role_id from sys_role_user where user_id = ?))";
        object = jdbc.selectForOneNode(sql, servletPath, userInfo.id);
        // 没有权限
        if (object == null) {


            String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
            String Cusername = UserService.getCurrentUserName();
            String username = UserService.getCurrentUserAccount();
            String ip = UserService.getCurrentUserIp();
            String content = "越权访问接口："+ servletPath;
            String type = "越权访问接口";
            LocalDateTime currentDateTime = LocalDateTime.now();
            String time = currentDateTime.toString();
            System.out.println(username);
            System.out.println(ip);
            System.out.println(content);
            System.out.println(type);
            jdbc.update(audit,Cusername,username,ip,content,type,time,"高");


        	printResult(response, new Result("没有后台接口"+servletPath +"的访问权限",401,"401"));



            return false;
        }

        //System.out.println("preHandle return true" );
        return true;
    }
 
}
