package com.ceit.admin.controller;

import com.alibaba.fastjson2.JSONArray;
import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller("/authPolicy")
public class AuthPolicyController {
    @Autowired
    private SimpleJDBC simpleJDBC;

    // 获取认证策略列表
    @RequestMapping("/getList")
    public List getList(Map<String, Object> reqBody) {
        String[] optionNames ={"name", "methods", "description", "is_default"};

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        return sqlUtil.setTable("sys_auth_policy")
                .setAcceptOptions(optionNames)
                .selectForMapList();
    }

    @RequestMapping("/getSettingList")
    public List getSettingList(Map<String, Object> reqBody) {
        String[] optionNames ={"policy_id"};

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        return sqlUtil.setTable("sys_auth_policy_setting")
                .setAcceptOptions(optionNames)
                .setOrderBy("policy_id,code")
                .selectForMapList();

    }

    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {
        Object o = reqBody.get("is_default");
        if (o == null){
            reqBody.put("is_default",0);
        } else {
            //当o不是空时，说明is_default的值为1
            String sql = "update sys_auth_policy set is_default = 0 where is_default = 1";
            simpleJDBC.update(sql);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("sys_auth_policy")
                .setFields("name","methods","description","is_default")
                .setSearchFields("id")
                .insert();

        Object obj = sqlUtil.setTable("sys_auth_policy")
                .setFields("id")
                .setSearchFields("name")
                .setOrderBy("id desc")
                .selectForOneNode();
        int id = obj!=null ? Integer.parseInt(obj.toString()) :null;
        String min_len = reqBody.get("min_len") != null ? reqBody.get("min_len").toString() : null;
        String max_len = reqBody.get("max_len") != null ? reqBody.get("max_len").toString() : null;
        String fail_lock_count = reqBody.get("fail_lock_count") != null ? reqBody.get("fail_lock_count").toString() : null;
        String fail_lock_time = reqBody.get("fail_lock_time") != null ? reqBody.get("fail_lock_time").toString() : null;
        String valid_days = reqBody.get("valid_days") != null ? reqBody.get("valid_days").toString() : null;
        String java_regex_x = reqBody.get("java_regex_x") != null ? reqBody.get("java_regex_x").toString() : null;
        String ip_auto_bind = reqBody.get("ip_auto_bind") != null ? reqBody.get("ip_auto_bind").toString() : null;
        String weekday = reqBody.get("weekday") != null ? reqBody.get("weekday").toString() : null;
        String daytime = reqBody.get("daytime") != null ? reqBody.get("daytime").toString() : null;

        if (ret != 0){
            String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
            String Cusername = UserService.getCurrentUserName();
            String username = UserService.getCurrentUserAccount();
            String name = reqBody.get("name").toString();
            String ip = UserService.getCurrentUserIp();
            String content = "添加了名称为："+name+"的认证策略";
            String type = "认证策略添加";
            LocalDateTime currentDateTime = LocalDateTime.now();
            String time = currentDateTime.toString();
            System.out.println(username);
            System.out.println(name);
            System.out.println(ip);
            System.out.println(content);
            System.out.println(type);
            simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");

            insertOrUpdatePolicySetting(id, "pwd", "min_len", min_len);
            insertOrUpdatePolicySetting(id, "pwd", "max_len", max_len);
            insertOrUpdatePolicySetting(id, "pwd", "fail_lock_count", fail_lock_count);
            insertOrUpdatePolicySetting(id, "pwd", "fail_lock_time", fail_lock_time);
            insertOrUpdatePolicySetting(id, "pwd", "valid_days", valid_days);
            insertOrUpdatePolicySetting(id, "pwd", "java_regex_x", java_regex_x);
            insertOrUpdatePolicySetting(id, "ip", "auto_bind", ip_auto_bind);
            insertOrUpdatePolicySetting(id, "time", "weekday", weekday);
            insertOrUpdatePolicySetting(id, "time", "daytime", daytime);
            return new Result("添加成功", 200, "success");
        }
        return new Result("添加失败", 200, "error");
    }

    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {
        Object o = reqBody.get("is_default");
        if (o == null){
            reqBody.put("is_default",0);
        } else {
            //当o不是空时，说明is_default的值为1
            String sql = "update sys_auth_policy set is_default = 0 where is_default = 1";
            simpleJDBC.update(sql);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("sys_auth_policy")
                .setFields("name","methods","description","is_default")
                .setWhere("id=?",reqBody.get("id"))
                .update();

        
        int id = reqBody.get("id") != null ? Integer.parseInt(reqBody.get("id").toString()) : null;
        String min_len = reqBody.get("min_len") != null ? reqBody.get("min_len").toString() : null;
        String max_len = reqBody.get("max_len") != null ? reqBody.get("max_len").toString() : null;
        String fail_lock_count = reqBody.get("fail_lock_count") != null ? reqBody.get("fail_lock_count").toString() : null;
        String fail_lock_time = reqBody.get("fail_lock_time") != null ? reqBody.get("fail_lock_time").toString() : null;
        String valid_days = reqBody.get("valid_days") != null ? reqBody.get("valid_days").toString() : null;
        String java_regex_x = reqBody.get("java_regex_x") != null ? reqBody.get("java_regex_x").toString() : null;
        String ip_auto_bind = reqBody.get("ip_auto_bind") != null ? reqBody.get("ip_auto_bind").toString() : null;
        String weekday = reqBody.get("weekday") != null ? reqBody.get("weekday").toString() : null;
        String daytime = reqBody.get("daytime") != null ? reqBody.get("daytime").toString() : null;

        if (ret != 0) {
            String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
            String Cusername = UserService.getCurrentUserAccount();
            String username = UserService.getCurrentUserName();
            String name = reqBody.get("name").toString();
            String ip = UserService.getCurrentUserIp();
            String content = "编辑了名称为："+name+"的认证策略";
            String type = "认证策略编辑";
            LocalDateTime currentDateTime = LocalDateTime.now();
            String time = currentDateTime.toString();
            System.out.println(username);
            System.out.println(name);
            System.out.println(ip);
            System.out.println(content);
            System.out.println(type);
            simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");
            insertOrUpdatePolicySetting(id, "pwd", "min_len", min_len);
            insertOrUpdatePolicySetting(id, "pwd", "max_len", max_len);
            insertOrUpdatePolicySetting(id, "pwd", "fail_lock_count", fail_lock_count);
            insertOrUpdatePolicySetting(id, "pwd", "fail_lock_time", fail_lock_time);
            insertOrUpdatePolicySetting(id, "pwd", "valid_days", valid_days);
            insertOrUpdatePolicySetting(id, "pwd", "java_regex_x", java_regex_x);
            insertOrUpdatePolicySetting(id, "ip", "auto_bind", ip_auto_bind);
            insertOrUpdatePolicySetting(id, "time", "weekday", weekday);
            insertOrUpdatePolicySetting(id, "time", "daytime", daytime);
            return new Result("修改成功", 200, "success");
        }
        return new Result("修改失败", 200, "error");
    }

    private boolean insertOrUpdatePolicySetting(int policy_id, String method_code, String code, String value) {
        if(value == null || value.length() == 0){
            return false;
        }
        String sql = "select value from sys_auth_policy_setting where policy_id=? and method_code=? and code=?";
        Object obj = simpleJDBC.selectForOneNode(sql, policy_id, method_code, code);
        if (obj != null) {
            if (!obj.toString().equals(value)) {
                sql = "update sys_auth_policy_setting set value=? where policy_id=? and method_code=? and code=?";
                int rSet = simpleJDBC.update(sql, value, policy_id, method_code, code);
                if (rSet == 0) {
                    return false;
                }
            }
            return true;
        }
        sql = "insert into sys_auth_policy_setting (policy_id,method_code,code,name,value) value (?,?,?,?,?)";
        simpleJDBC.update(sql,policy_id,method_code,code,"",value);
        return false;
    }

    @RequestMapping("/delete")
    public Result delete(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] ids = str.split(",");
        int rSet = 0;
        for (int i = 0; i < ids.length; i++) {
            Integer id = Integer.parseInt(ids[i]);
            rSet = simpleJDBC.update("delete from sys_auth_policy where id=?", id);
            simpleJDBC.update("delete from sys_auth_policy_setting where policy_id=?", id);
        }
        if (rSet != 0) {

            String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
            String Cusername = UserService.getCurrentUserName();
            String username = UserService.getCurrentUserAccount();
            String name = reqBody.get("name").toString();
            String ip = UserService.getCurrentUserIp();
            String content = "删除了名称为："+name+"的认证策略";
            String type = "认证策略删除";
            LocalDateTime currentDateTime = LocalDateTime.now();
            String time = currentDateTime.toString();
            System.out.println(username);
            System.out.println(name);
            System.out.println(ip);
            System.out.println(content);
            System.out.println(type);
            simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");
            return new Result("删除成功", 200, "success");
        }
        return new Result("删除失败", 200, "error");
    }
}
