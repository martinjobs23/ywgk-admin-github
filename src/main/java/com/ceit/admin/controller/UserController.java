package com.ceit.admin.controller;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.ceit.admin.common.utils.SM3Utils;
import com.ceit.admin.service.PasswordService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;

@Controller("/user")
public class UserController {

    @Autowired
    private SimpleJDBC simpleJDBC;

    @Autowired
    PasswordService passwordService;

    private final String[] optionNames = { "account", "name", "id_number", "email", "tel", "mobile" };
    private final String searchFiledName = "org_id";
    private final String tableName = "sys_user";

    /**
     * 树形展示数组
     */
    @RequestMapping("/tree")
    public Result treeData(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        List jsonData = sqlUtil.setTable(tableName)
                .setSearchFields(searchFiledName)
                .setAcceptOptions(optionNames)
                .selectForMapList();

        return new Result(ResultCode.SUCCESS_TOTREE, jsonData);
    }

    /**
     * 不分页列表数组
     */
    @RequestMapping("/list")
    public Result listData(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        List jsonData = sqlUtil.setTable(tableName)
                .setSearchFields(searchFiledName)
                .setAcceptOptions(optionNames)
                .setWhere("sys_user.disabled = 0")
                .selectForMapList();

        return new Result("ok", 200, jsonData);
    }

    /**
     * 分页列表数组
     */
    @RequestMapping("/page")
    public Result page(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("sys_user " +
                        "left join sys_user_password " +
                        "on sys_user.id=sys_user_password.user_id " +
                        "left join sys_user_ip " +
                        "on sys_user.id=sys_user_ip.user_id ")
                .setFields("sys_user.*,sys_user_password.locked,sys_user_ip.ip,sleep,disabled")
                .setSearchFields(searchFiledName)
                .setAcceptOptions(optionNames)
                .setWhere("sys_user.disabled = 0");

        return sqlUtil.selectForTotalRowsResult();
    }

    /**
     * 单个对象数据
     */
    @RequestMapping("/get")
    public Result get(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        JSONObject jsonData = sqlUtil.setTable(tableName)
                .setSearchFields(searchFiledName)
                .setAcceptOptions(optionNames)
                .selectForJsonObject();
        return new Result("ok", 200, jsonData);
    }

    /**
     * 获取绑定的IP地址
     */
    @RequestMapping("/getIP")
    public Result getIP(Map<String, Object> reqBody) {
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        JSONObject jsonData = sqlUtil.setTable("sys_user_ip")
                .setFields("ip")
                .setSearchFields("user_id")
                .selectForJsonObject();
        return new Result("ok", 200, jsonData);
    }

    /**
     * 获取绑定的微信号
     */
    @RequestMapping("/getWx")
    public Result getWx(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        JSONObject jsonData = sqlUtil.setTable("sys_user_wx")
                .setFields("wx")
                .setSearchFields("user_id")
                .selectForJsonObject();
        return new Result("ok", 200, jsonData);
    }

    /**
     * 获取绑定的MAC地址
     */
    @RequestMapping("/getMAC")
    public Result getMAC(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        JSONObject jsonData = sqlUtil.setTable("sys_user_mac")
                .setFields("mac")
                .setSearchFields("user_id")
                .selectForJsonObject();
        return new Result("ok", 200, jsonData);
    }

    /**
     * 添加IP绑定
     */
    @RequestMapping("/insertIP")
    public Result insertIP(Map<String, Object> reqBody) {
        String ip = reqBody.get("bindData").toString();
        Integer user_id = Integer.parseInt(reqBody.get("user_id").toString());
        String sql = "insert into sys_user_ip (user_id, ip) values(?,?)";
        int ret = simpleJDBC.update(sql, user_id, ip);
        if (ret > 0) {
            return new Result("修改成功", 200, "success");
        }
        return new Result("修改失败", 200, "error");
    }

    /**
     * 添加MAC地址绑定
     */
    @RequestMapping("/insertMAC")
    public Result insertMAC(Map<String, Object> reqBody) {
        String mac = reqBody.get("bindData").toString();
        Integer user_id = Integer.parseInt(reqBody.get("user_id").toString());
        String sql = "insert into sys_user_mac (user_id, mac) values(?,?)";
        int ret = simpleJDBC.update(sql, user_id, mac);
        if (ret > 0) {
            return new Result("修改成功", 200, "success");
        }
        return new Result("修改失败", 200, "error");
    }

    private String encryptPassword(String password) {
        String encrypt = "{SM3}" + SM3Utils.encrypt(password); // 将密码进行sm3加密并拼接
        return encrypt;
    }

    private Result insertOrUpdatePassword(Integer user_id, String newpass) {

        //验证密码复杂度
        Result result = passwordService.complexityCheck(user_id, newpass);
        if (!result.getData().equals("success")) {
            return result;
        }

        // 密码是否存在
        String sql = "select count(*) from sys_user_password where user_id=?";
        Object count= simpleJDBC.selectForOneNode(sql, user_id) ;

        if (count == null ||  (Long)count == 0) {
            sql = "insert into sys_user_password "
                    + "(password, user_id, change_time, locked) "
                    + "VALUES(?, ?, NOW(), 0);";
        } else {
            sql = "update sys_user_password set password=?, change_time =NOW() where user_id=?";
        }

        int ret = simpleJDBC.update(sql, encryptPassword(newpass), user_id);
        if (ret > 0) {
            result.setData("success");
            result.setMsg("设置密码成功");
            return result;
        }

        result.setData("error");
        result.setMsg("写入数据库失败");
        return result;
    }
    
    //初次登录设置新密码
    @RequestMapping("/changePassword")
    public Result setPassword(Map<String, Object> reqBody){
 
        String type =(String) reqBody.get("type");
        String account = reqBody.get("username") == null ? "": reqBody.get("username").toString();
        int id = simpleJDBC.selectForOneInt("SELECT id FROM `sys_user` WHERE account = ?", account);
        if(id <= 0){
            return new Result(400,"获取用户信息失败","error");
        }
 
        //需要验证原密码
        if(type == "2" || type.equals("2")){
        	
            String oldpassword_encypted =(String) reqBody.get("oldpassword");
            if(oldpassword_encypted ==null || oldpassword_encypted.isEmpty())
           	 	return new Result(200,"原密码不能为空！","error");
            
            //SM2解密
            String oldpassword = PasswordService.sm2Decrypt(oldpassword_encypted);
            if(oldpassword ==null || oldpassword.isEmpty())
            	 return new Result(200,"原密码格式错误或解密失败！","error");
            
            //验证原密码是否正确
            String oldpassword_sm3 = "{SM3}" + SM3Utils.encrypt(oldpassword);
            
            String sql = "select count(*) from sys_user_password where user_id=? and password=?";
            int match = simpleJDBC.selectForOneInt(sql, id, oldpassword_sm3);
            if(match<=0)
            	return new Result(200,"原密码验证失败，不能修改密码！","error");
         }

        //新密码
        String newpassword_encypted =(String) reqBody.get("password");
        if(newpassword_encypted ==null || newpassword_encypted.isEmpty())
       	 	return new Result(200,"新密码不能为空！","error");
 
        //SM2解密
        String newpassword = PasswordService.sm2Decrypt(newpassword_encypted);
        if(newpassword ==null || newpassword.isEmpty())
        	 return new Result(200,"新密码格式错误或解密失败！","error");
        
         //验证密码复杂度
         Result result = passwordService.complexityCheck(id, newpassword);
         if (!result.getData().equals("success")) {
        	 return new Result(200,"新密码不符合密码复杂度要求！","error");
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

    /**
     * 修改密码
     */
    @RequestMapping("/updatePassword")
    public Result updatePassword(Map<String, Object> reqBody) {

        Integer user_id = Integer.parseInt(reqBody.get("id").toString());
        String password = reqBody.get("password").toString();

        //新密码
        String newpassword_encypted =(String) reqBody.get("password");
        if(newpassword_encypted ==null || newpassword_encypted.isEmpty())
       	 	return new Result(401,"新密码不能为空！","error");
 
        //SM2解密
        String newpassword = PasswordService.sm2Decrypt(newpassword_encypted);
        if(newpassword ==null || newpassword.isEmpty())
        	 return new Result(401,"新密码格式错误或解密失败！","error");

        Result result = insertOrUpdatePassword(user_id, password);
        if (result.getData().equals("success")) {
            return new Result("修改成功", 200, "success");
        }

        return result;
    }

    /**
     * 修改绑定的IP地址
     */
    @RequestMapping("/updateIP")
    public Result updateIP(Map<String, Object> reqBody) {
        String ip = reqBody.get("bindData").toString();
        Integer user_id = Integer.parseInt(reqBody.get("user_id").toString());
        String sql = "update sys_user_ip set ip = ? where user_id = ?";
        int ret = simpleJDBC.update(sql, ip, user_id);
        if (ret > 0) {
            return new Result("修改成功", 200, "success");
        }
        return new Result("修改绑定的IP地址失败", 200, "error");
    }

    /**
     * 修改绑定的IP地址
     */
    @RequestMapping("/updateMAC")
    public Result updateMAC(Map<String, Object> reqBody) {
        String mac = reqBody.get("bindData").toString();
        Integer user_id = Integer.parseInt(reqBody.get("user_id").toString());
        String sql = "update sys_user_mac set mac = ? where user_id = ?";
        int ret = simpleJDBC.update(sql, mac, user_id);
        if (ret > 0) {
            return new Result("修改成功", 200, "success");
        }
        return new Result("修改绑定的MAC地址失败", 200, "error");
    }

    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {

        if (reqBody.get("account") == null)
            return new Result("修改失败:账号不能为空", 200, "error");

        // account不能重复,或者不允许修改账号
        Object obj = simpleJDBC.selectForOneNode("SELECT count(*) FROM sys_user WHERE account=?",
                reqBody.get("account"));
        if (obj != null && (Long) obj > 0) {
            return new Result("修改失败:账号已存在", 200, "error");
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String create_time = df.format(System.currentTimeMillis());
        reqBody.put("create_time", create_time);
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        Integer user_id = sqlUtil.setTable("sys_user")
                .setFields("account", "name", "id_number", "description", "email", "org_id", "sex", "creator_id",
                        "disabled", "create_time", "start_time", "end_time", "mobile")
                .setSearchFields("id")
                .insertAutoIncKey();
        
        // 添加密码
        String newpass = (String) reqBody.get("password");
        if (newpass != null && newpass.length() > 0) {
            Result result = insertOrUpdatePassword(user_id, newpass);
            if (!result.getData().equals("success")) {
                return result;
            }
        }
        // 添加绑定的ip地址
        System.out.println("======user_iduser_id======");
        System.out.println(user_id);
        String ip = (String) reqBody.get("ip");
        if (ip != null && ip.length() > 0) {
            String sql = "insert into sys_user_ip (user_id, ip) values(?,?)";
            int ret = simpleJDBC.update(sql, user_id, ip);
        }

        return new Result("ok", 200, user_id);
    }

    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {
        int id = Integer.parseInt(reqBody.get("id").toString());

        // account不能重复,或者不允许修改账号
        if (reqBody.get("account") != null) {
            Object obj = simpleJDBC.selectForOneNode("SELECT count(*) FROM sys_user WHERE id<>? AND account=?",
                    id, reqBody.get("account"));
            if (obj != null && (Long) obj > 0) {
                return new Result("修改失败:账号已存在", 500, "error");
            }
        }

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("sys_user")
                .setFields("account", "name", "id_number", "description", "email", "org_id", "sex", "creator_id",
                        "disabled","sleep", "create_time", "start_time", "end_time", "mobile")
                .setWhere("id=?",reqBody.get("id"))
                .update();

        //修改用户锁定状态
        System.out.println(reqBody);
        SqlUtil sqlUtil1 = new SqlUtil(reqBody);
        int ret1 = sqlUtil1.setTable("sys_user_password")
                .setFields("locked")
                .setWhere("user_id="+id)
                .update();

        // 修改密码
        String newpass = (String) reqBody.get("password");
        newpass = PasswordService.sm2Decrypt(newpass);
        if (newpass != null && newpass.length() > 0) {
            Integer user_id = Integer.parseInt(reqBody.get("id").toString());
            Result result = insertOrUpdatePassword(user_id, newpass);
            if (!result.getData().equals("success")) {
                return result;
            }
        }
        // 修改绑定的ip地址
        String ip = (String) reqBody.get("ip");
        if (ip != null && ip.length() > 0) {
            SqlUtil sqlUtil2 = new SqlUtil(reqBody);
            int ret2 = sqlUtil2.setTable("sys_user_ip")
                    .setFields("ip")
                    .setWhere("user_id="+id)
                    .update();
        }

        return new Result("ok", 200, ret);
    }

    @RequestMapping("/delete")
    public Result delete(Map<String, Object> reqBody) {
        String str = reqBody.get("ids").toString();
        String[] userIds = str.split(",");
        int rSet = 0;
        for (int i = 0; i < userIds.length; i++) {
            Integer id = Integer.parseInt(userIds[i]);
//            rSet = simpleJDBC.update("delete from sys_user where id=?", id);
            //修改删除逻辑，把要删除的用户改成 disable =1  已注销状态
            rSet = simpleJDBC.update("update sys_user set disabled = 1 where id = ?", id);
        }
        return new Result("success", 200, rSet);

    }
}
