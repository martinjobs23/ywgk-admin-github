package com.ceit.admin.service;


import com.ceit.admin.common.utils.SM2Utils;
import com.ceit.admin.common.utils.SM3Utils;
import com.ceit.admin.model.UserInfo;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Component;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.alibaba.fastjson2.JSONObject;

@Component
public class PasswordService {
    
    @Autowired
    private SimpleJDBC simpleJDBC;

    /**
     *  账号密码登录
     * @param reqBody
     * @param request
     * @return
     */
    public Result passwordLogin(Map<String, Object> reqBody, HttpServletRequest request){
        Result result = new Result();
        result.setCode(401);
        String account  = (String) reqBody.get("account");
        String password  = (String) reqBody.get("password");

        // sm2解密
        Date nowDate = new Date();
        password=sm2Decrypt(password);

        //1.用户名、密码是否为空
        if (account == null || account.equals("") || password == null || password.equals("")){
            result.setMsg("用户名或密码不能为空");
            result.setData("error");
            return result;
        }
        /**
         * 分开查找用户 先看有无账号，再看有无密码
         */
        String str = "select * from sys_user where account = ?";
        Map<String, Object> map = simpleJDBC.selectForMap(str, account);
        if (map==null){
            result.setMsg("访问数据库失败，请重试或联系管理员");
            result.setData("error");
            return result;
        }

        if (map.isEmpty()){
            result.setMsg("用户名或密码错误");
            result.setData("error");
            return result;
        }

        String sql = "select u.*, up.password, up.locked, up.change_time, up.fail_count, up.fail_time from sys_user u join sys_user_password up where account = ? and u.id = up.user_id";
        Map<String, Object> userMap = simpleJDBC.selectForMap(sql, account);
        //2.账号是否存在
        if (userMap.isEmpty()){
            result.setMsg("此账号未设置密码，请联系管理员");
            result.setData("error");
            return result;
        }
        //3.查找用户的认证策略 --是否需要密码登录
        Result policyCheckRes = policyCheck(userMap, "pwd", account, request);
        if (policyCheckRes.getMsg() != null && !policyCheckRes.getMsg().equals("")){
            return policyCheckRes;
        }
        UserInfo userInfo = (UserInfo) policyCheckRes.getData();
        // 以下是密码登录方式的检验
        // 4.查看用户是否禁用
        if ((Integer) userMap.get("disabled") == 1){
            result.setMsg("账号已被禁用");
            result.setData("error");
            return result;
        }
        // 5.用户是否锁定
        if ((Integer)userMap.get("locked") == 1){
            Map isUnlocked = isUnLocked(userInfo.id, (Date) userMap.get("fail_time"));
            if (isUnlocked != null){
                result.setMsg((String) isUnlocked.get("msg"));
                result.setData("error");
                return result;
            }
        }
        /*
        * 现在需要修改
         */
        //6.用户密码是否过期
//        sql = "select value from sys_auth_policy_setting where policy_id = 1 and method_code = 'pwd' and code = 'valid_days'";
//        Object authPolicySetting = simpleJDBC.selectForOneNode(sql);
//        //若有设置，再判断是否过期
//        if (authPolicySetting != null){
//            Integer value = Integer.parseInt(authPolicySetting.toString());
//            Date changeTime = (Date) userMap.get("change_time");
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(changeTime);
//            calendar.add(Calendar.DATE, value);
//            if (calendar.getTime().getTime() < nowDate.getTime()) {
//                result.setMsg("密码已过期");
//                result.setData("error");
//                return result;
//            }
//        }
        //6.5 用户账号是否为休眠账号
        if (userMap.get("login_time") != null){
            LocalDateTime  loginData = (LocalDateTime) userMap.get("login_time");
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zdt = loginData.atZone(zoneId);
            Date date = Date.from(zdt.toInstant());
            boolean sleep = CheckUnusedUsers((int) userMap.get("id"),date);
            if (sleep){
                result.setMsg("此账号长时间未登录，已休眠");
                result.setData("error");
                return result;
            }
        }
        if(userMap.get("sleep") != null){
            if (Integer.valueOf(userMap.get("sleep").toString()) == 1){
                result.setMsg("此账号长时间未登录，已休眠");
                result.setData("error");
                return result;
            }
        }
        //7.密码是否正确
        //7.1截取密码中{SM3}后面的字符串
        String realPassword = (String) userMap.get("password");
        realPassword = realPassword.substring(5);
        //7.2获取用户输入的密码的sm3加密值
        String encrypt = SM3Utils.encrypt(password);
        if (!realPassword.equals(encrypt)) {
        	
        	int left_fail_count = -1;
        	
            //密码错误则更新数据库
            String ip = getClientIp(request);//失败ip
            sql = "update sys_user_password set fail_count = fail_count+1,  fail_ip = ?, fail_time = ? where user_id = ?";
            simpleJDBC.update(sql, ip, nowDate, userInfo.id);
            
            //8.账号是否需要锁定
            sql = "select value from sys_auth_policy_setting where policy_id = 1 and method_code = 'pwd' and code = 'fail_lock_count'";
            int fail_lock_count = simpleJDBC.selectForOneInt(sql);
            if (fail_lock_count > 0){
                sql = "select fail_count from sys_user_password where user_id = ?";
                int failCount = simpleJDBC.selectForOneInt(sql, userInfo.id);
                if(failCount >=0)
                	left_fail_count = fail_lock_count - failCount ;
                
                //若错误次数 >= 最大错误次数 锁定账号
                if(left_fail_count<=0) {
                    sql = "update sys_user_password set locked = 1 where user_id = ?";
                    simpleJDBC.update(sql, userInfo.id);
                    
                    result.setMsg("用户名或密码错误，账号已被锁定");
                    return result;
                }
            }
            
            if (isVerifyCode(Integer.parseInt(userMap.get("fail_count").toString()) + 1)){
                result.setData("true");
            }
            
            if(left_fail_count>0)
            	result.setMsg("用户名或密码错误,再错误"+left_fail_count+"次账号将被锁定");
            else
            	result.setMsg("用户名或密码错误");
            
        } else {
            //9.登录成功，修改失败次数等字段...
            sql = "update sys_user_password set locked = 0,fail_count = 0,fail_ip = null,fail_time = null where user_id = ?";
            simpleJDBC.update(sql, userInfo.id);

            //设置用户属性 
            userInfo.setOkAuthPolicy("pwd");
            userInfo.account = (String)userMap.get("account");
            userInfo.name = (String)userMap.get("name");
            userInfo.clientIp = getClientIp(request);

            request.getSession().setAttribute("userInfo",userInfo);
            
            //检查是否所有认证方式均完成
            boolean res = userInfo.checkAuth();
            if (res == false){
                //result.setCode(100);
                //result.setData(userInfo.unAuthPolicy.get(0)); //将没有认证的方式路径发送给前端


                for (String s : userInfo.unAuthPolicy) {
                    if(s.equals("ip")){
                        if(!checkip(userInfo)){
                            return new Result(401,"未在允许的ip地址登录","error");
                        }
                    }else if(s.equals("time")){
                        if(!checktime(userInfo)){
                            return new Result(401,"未在允许的时间登录","error");
                        }
                    }
                }


                //设置线程对象
                UserService.clientUserId.set(String.valueOf(userInfo.id));
                UserService.clientUserAccount.set(userInfo.account);
                UserService.clientUserName.set(userInfo.name);
                UserService.clientUserIP.set(userInfo.clientIp);

                String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
                String Cusername = UserService.getCurrentUserName();
                String username = UserService.getCurrentUserAccount();
                String ip = UserService.getCurrentUserIp();
                String content = "用户"+username+"登录成功";
                String type = "用户登录";
                LocalDateTime currentDateTime = LocalDateTime.now();
                String time = currentDateTime.toString();
                System.out.println(Cusername);
                System.out.println(username);
                System.out.println(ip);
                System.out.println(content);
                System.out.println(type);
                simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"低");
            }
            String data = "{\"accessToken\":\""+account+"\"}";
            
            result.setCode(200);
            result.setData(data);

            //所有认证方式都完成
            request.getSession().setAttribute("userid",userInfo.id);
            request.getSession().setAttribute("username",userInfo.account);
        }

        return result;
    }
    /**
     * 检查ip绑定
     */
    public boolean checkip(UserInfo userInfo){
        String sql_ips = "SELECT ip FROM sys_user_ip WHERE user_id = ?";
        String insert_ip = "INSERT into sys_user_ip (user_id,ip,bind_time,auto_bind) VALUES (?,?,NOW(),0)";

        String ips = simpleJDBC.selectForOneString(sql_ips,userInfo.id);
        if(ips == null || ips.isEmpty()){
            simpleJDBC.update(insert_ip,userInfo.id,userInfo.clientIp);
            return true;
        }else if(ips.contains(userInfo.clientIp)){
            return true;
        }else{
            return false;
        }
    }
    public int getPolicy_id (int user_id){
        String sql = "select id from sys_auth_policy where id in (select auth_policy_id from sys_role where id in (select role_id from sys_role_user where user_id = ?))";
        int id = simpleJDBC.selectForOneInt(sql, user_id);
        return id;
    }

    /**
     * 检查时间
     */
    public boolean checktime(UserInfo userInfo){

        int policy_id = getPolicy_id(userInfo.id);
        String sql = "SELECT value FROM sys_auth_policy_setting WHERE code = \"daytime\" and policy_id = ?";
        String time = simpleJDBC.selectForOneString(sql,policy_id);

        if(time == null || time.isEmpty()){
            return true;
        }

        String[] s = time.split("-");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime currentTime = LocalTime.now();
        LocalTime time1 = LocalTime.parse(s[0], formatter);
        LocalTime time2 = LocalTime.parse(s[1], formatter);
        boolean isBetween = currentTime.isAfter(time1) && currentTime.isBefore(time2);
        if (isBetween) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否是首次登录
     */
    public Result isFirstLogin(Map<String, Object> reqBody){
        /*
            是：返回
            否：登录成功
        */
        String sql_fistLogin = "select login_time from sys_user where account = ?";
        String sql_updateLogintime = "update sys_user set login_time = ? where account = ?";
        String sql_queryPasswordValidDays = "select value from sys_auth_policy_setting where code = \"valid_days\" and policy_id = ?";
        String sql_querydifferenceDays = "SELECT TIMESTAMPDIFF(DAY,sys_user_password.change_time,NOW()) FROM sys_user_password WHERE user_id = ?";
        String sql_queryID = "select id from sys_user where account = ?";
        String redirect;
        String redir;
        JSONObject jsonObject = new JSONObject();

        String account =(String) reqBody.get("account");
        System.out.println("account:"+account);
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = localDateTime.format(formatter);

        //isfirstlogin:2023-03-01T19:22:03
        String isfirstlogin = simpleJDBC.selectForOneString(sql_fistLogin,account);
        if(isfirstlogin == null || isfirstlogin.equals("") || isfirstlogin.length() == 0 || isfirstlogin.isEmpty()){
            // 初次登录
            System.out.println("初次登陆");
            simpleJDBC.update(sql_updateLogintime,formattedTime,account);
            redir = "changepassword";
        }else{
            // 非
            System.out.println("非初次登录");
            redir = "index" ;

            // 此处可增加判断：该用户是否是三个月未修改密码，是的话仍跳转至修改密码界面
            Integer id = Integer.parseInt(simpleJDBC.selectForOneString(sql_queryID,account));
            Integer differenceDays = Integer.parseInt(simpleJDBC.selectForOneString(sql_querydifferenceDays,id));

            int policy_id = getPolicy_id(id);
            Integer valid_days = Integer.parseInt(simpleJDBC.selectForOneString(sql_queryPasswordValidDays,policy_id));
            if(differenceDays > valid_days){
                redir = "changepassword";
            }
            simpleJDBC.update(sql_updateLogintime,formattedTime,account);
        }
        jsonObject.put("redirect",redir);
        redirect = jsonObject.toJSONString();

        Result result = new Result(200,"success",redirect);
        System.out.println("result-----------------------: "+result);
        return result;
    }

    /**
     *  公共部分的验证
     * @param userMap
     * @param method
     * @param authString
     * @param request
     * @return
     */
    public Result policyCheck(Map<String, Object> userMap, String method, String authString, HttpServletRequest request){
        Result result = new Result();
        result.setCode(401);
        String data = "error";
        Integer id = Integer.parseInt(userMap.get("id").toString());
        String sql = "select methods from sys_auth_policy where id in (select auth_policy_id from sys_role where id in (select role_id from sys_role_user where user_id = ?))";
        Object methods = simpleJDBC.selectForOneNode(sql, id);
        if (methods == null){
            //是因为什么没找到策略
            //1.第一种情况：因为没有为用户分配角色
            sql = "select * from sys_role_user where user_id = ? ";
            List<Map<String, Object>> maps = simpleJDBC.selectForMapList(sql, id);
            if (maps.size() == 0){
                return new Result("没有为用户分配角色，请联系管理员为您分配角色",200,"error");
            }
            //2.第二种情况：因为没有为角色分配策略，使用默认策略
            sql = "select methods from sys_auth_policy where is_default = 0";
            methods = simpleJDBC.selectForOneNode(sql);
            //没有默认策略，拒绝登陆
            if (methods == null){
                result.setMsg("没有未用户分配角色或者认证策略，拒绝登录");
                result.setData(data);
                return result;
            }
        }
        HttpSession session = request.getSession();
        UserInfo user = new UserInfo();
        Object userInfo = session.getAttribute("userInfo");
        if (userInfo != null){
            user = (UserInfo) userInfo;

            if (method.equals("pwd") && !user.account.equals(authString)){
                //之前已经登录，但是没有注销，又重新登录 但是新用户账号原来不同
                result.setMsg("认证失败：账号已登录，请注销当前用户再重新登录"); //根据登录方式返回不同的错误值
                result.setData(data);
                return result;
            }
            if (method.equals("sms")){
                sql = "select mobile from sys_user where id = ?";
                String mobile = (String) simpleJDBC.selectForOneNode(sql, user.id);
                if (!mobile.equals(authString)){
                    result.setMsg("认证失败：手机号错误");
                    result.setData(data);
                    return result;
                }
            }
            if (method.equals("wx")){
                sql = "select wx from sys_user_wx where user_id = ?";
                String wx = (String) simpleJDBC.selectForOneNode(sql, user.id);
                if (!wx.equals(authString)) {
                    result.setMsg("认证失败：微信号错误");
                    result.setData(data);
                    return result;
                }
            }
        } else {
            user.id = id;
            user.account = userMap.get("account").toString();
            user.setAllAuthMethod((String) methods);
            user.setNode((String) methods);
        }
        if (!user.allAuthPolicy.contains(method)){
            result.setMsg("登录方式错误，请使用其它方式登录");
            result.setData(data);
            return result;
        }
        
        result.setCode(200);
        result.setData(user);
        return result;
    }
    /**
     * 查看用户是否需要某种方法登录
     * @param userInfo
     * @param method
     * @return
     */
    public boolean includeAuthPolicy(UserInfo userInfo, String method){
        if (userInfo.allAuthPolicy.contains(method)){
            return true;
        }
        return false;
    }

    /**
     * 查看密码错误之后是否需要验证码
     * @param failCount
     * @return
     */
    public boolean isVerifyCode(Integer failCount) {
        String sql = "select value from sys_auth_policy_setting where  policy_id = 1 and method_code = 'pwd' and code = 'fail_captcha_count'";
        Object o = simpleJDBC.selectForOneNode(sql);
        if (o != null){
            if (failCount >= Integer.parseInt(o.toString())){
                return true;
            }
        }
        return false;
    }

    /**
     * 查看用户是否长期未登录，检查是否需要设置成休眠用户
     */
    public boolean CheckUnusedUsers(Integer userId, Date loginTime){
        Date nowDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(loginTime);
        calendar.add(Calendar.DATE, 90);
        if (calendar.getTime().getTime() < nowDate.getTime()) {
            System.out.println("用户需要设置成休眠");
            int rSet = simpleJDBC.update("update sys_user set sleep = 1 where id = ?", userId);
            return true;
        }
        return false;
    }

    //获取IP地址 linux 和Windows均适用
    public String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("IP地址获取失败" + e.toString());
        }
        return "";
    }


    //获取客户端IP
    public static String getClientIp(HttpServletRequest request)
    {
    	String ip = request.getHeader("X-Real-IP");
    	if(ip!=null)
    		return ip;
    				
    	ip = request.getHeader("X-Forwarded-For");
    	if(ip!=null)
    		return ip;
    	
    	ip = request.getRemoteAddr();
    	
    	return ip;
    }
    
    /**
     *
     * @Author 苏钰玲
     * @Description:账号是否可以解锁
     * @Date  2021/3/11
     **/
    public Map<String, String> isUnLocked(Integer userId, Date failTime){
        Map<String, String> map = null;
        String sql = "select value from sys_auth_policy_setting where policy_id = 1 and method_code = 'pwd' and code = 'fail_lock_time'";
        Integer value = Integer.parseInt(simpleJDBC.selectForOneNode(sql).toString());
        Date nowDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(failTime); //设置时间
        calendar.add(Calendar.SECOND,value);
        failTime = calendar.getTime();  //得到解锁时间
        if (failTime.getTime() < nowDate.getTime()){
            //可以解锁，更新数据库
            sql = "update sys_user_password set locked = 0,fail_count = 0,fail_ip = null,fail_time = null where user_id = ?";
            simpleJDBC.update(sql, userId);
        }else {
            map = new HashMap<>();
            int diffTime = (int) Math.ceil(1.0*(failTime.getTime() - nowDate.getTime())/(1000*60));
            map.put("msg","账户已被锁定，请"+diffTime+"分钟之后再次尝试");
        }
        return map;
    }

    public Result complexityCheck(Integer user_id, String newpass) {
        Result result = new Result();
        result.setCode(200);
        result.setData("error");

        //密码不能包含账号
        String sql = "select account from sys_user where id = ?";
        Object obj = simpleJDBC.selectForOneNode(sql, user_id);
        //未找到用户，不能修改密码
        if (obj == null) {
            result.setMsg("不存在该用户，请联系管理员");
            return result;
        }
        String account = obj.toString();
        if (newpass.contains(account)) {
            result.setMsg("密码不能为用户名或包含用户名");
            return result;
        }

        //新密码不能和旧密码相同
        sql = "select password from sys_user_password where user_id = ?";
        obj = simpleJDBC.selectForOneNode(sql, user_id);
        //存在旧密码
        if (obj != null) {
            String oldpass = obj.toString();
            if (oldpass.equals("{SM3}" + SM3Utils.encrypt(newpass))) {
                result.setMsg("新密码不能与旧密码相同");
                return result;
            }
        }

        //添加新用户设置密码时存在没有分配角色的情况，允许用户设置密码，此时采用默认策略
        int policy_id = 0;
        sql = "select id from sys_auth_policy where id in (select auth_policy_id from sys_role where id in (select role_id from sys_role_user where user_id = ?))";
        Object id = simpleJDBC.selectForOneNode(sql, user_id);
        if (id != null) {
            policy_id = Integer.parseInt(id.toString());
        }
        //查询密码复杂度策略
        sql = "SELECT VALUE FROM sys_auth_policy_setting WHERE policy_id = ? AND method_code = 'pwd' AND code = 'min_len' UNION SELECT VALUE FROM\tsys_auth_policy_setting WHERE\tpolicy_id = ? AND method_code = 'pwd' AND code = 'max_len' UNION SELECT VALUE FROM\tsys_auth_policy_setting WHERE\tpolicy_id = ? AND method_code = 'pwd' AND code = 'java_regex_x'";
        List<Object> list = simpleJDBC.selectForList(sql, policy_id, policy_id, policy_id);
        //没有复杂度策略
        if (list == null || list.size() < 3) {
            result.setMsg("不存在密码复杂度策略，请联系管理员");
            return result;
        }
        int min = Integer.parseInt(list.get(0).toString());
        int max = Integer.parseInt(list.get(1).toString());
        String regex = list.get(2).toString();
        //判断密码长度
        if (newpass.length() < min || newpass.length() > max) {
            result.setMsg("密码长度应为" + min + "到" + max + "位");
            return result;
        }
        //判断密码中的大小写、数字、特殊字符
        String regexResult = checkRegex(regex, newpass);
        if (!regexResult.equals("success")) {
            if (regex.equals("1111")) {
                result.setMsg(regexResult + "(密码中应包含大写字母、小写字母、数字和特殊字符)");
            } else if (regex.equals("0111")) {
                result.setMsg(regexResult + "(密码中应包含小写字母、数字和特殊字符)");
            } else if (regex.equals("1011")) {
                result.setMsg(regexResult + "(密码中应包含大写字母、数字和特殊字符)");
            } else if (regex.equals("1101")) {
                result.setMsg(regexResult + "(密码中应包含大写字母、小写字母和特殊字符)");
            } else if (regex.equals("1110")) {
                result.setMsg(regexResult + "(密码中应包含大写字母、小写字母和数字)");
            }
            return result;
        }
        result.setMsg("密码符合要求");
        result.setData("success");
        return result;
    }

    public String checkRegex(String regex, String newpass) {
        //判断密码中的大小写、数字、特殊字符
        if (regex.charAt(0) == '1') {
            if (!newpass.matches(".*[A-Z].*")) {
                return "密码中不含大写字母";
            }
        }
        if (regex.charAt(1) == '1') {
            if (!newpass.matches(".*[a-z].*")) {
                return "密码中不含小写字母";
            }
        }
        if (regex.charAt(2) == '1') {
            if (!newpass.matches(".*\\d.*")) {
                return "密码中不含数字";
            }
        }
        if (regex.charAt(3) == '1') {
            if (!newpass.matches(".*[^A-Za-z0-9].*")) {
                return "密码中不含特殊字符";
            }
        }
        return "success";
    }
    public static String sm2Decrypt(String ci){
        //        sm2解密
        String SM2_PRIVATE_KEY ="5ea65244fce1b1b662044539e035b6730c811009a6ea40fc869a0e7c02b426de";
        String SM2_PUBLIC_KEY = "04dbad1b2252d98d00f6f6821ef5183e593f98cb75eb91d2ec05461f6d39c04847db4a9d3f4edcb39521a4d94d5d8d6fddcb2c0eec46447e3c11088dea9bd6bd33";
        String decrypy_str="";
        try {
            decrypy_str=SM2Utils.decrypt(SM2_PRIVATE_KEY,"04"+ci);
        }
        catch (Exception e){
            System.out.println(e.toString());
        }
        return decrypy_str;
    }
}
