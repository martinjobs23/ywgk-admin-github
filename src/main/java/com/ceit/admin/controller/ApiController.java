package com.ceit.admin.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONArray;
import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.json.JSON;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;

@Controller("/api")
public class ApiController {

    @Autowired
    private SimpleJDBC simpleJDBC;
    private final String[] optionNames ={"name", "path", "description"};
    private final String searchFiledName = "id";
    private final String tableName = "sys_api";
    @Autowired
    private JSON json;
 


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
   
    @RequestMapping("/page")
    public Result page(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable(tableName)
                .setSearchFields(searchFiledName)
                .setAcceptOptions(optionNames);

        return sqlUtil.selectForTotalRowsResult();
    }

    // insert api
    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {
        if (reqBody.get("children") != null) {
            reqBody.remove("children");
        }
        // 一级菜单需将pid设为0
        if (reqBody.get("pid") == null) {
            reqBody.put("pid", 0);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        System.out.println("reqBody:"+reqBody);
        int ret = sqlUtil.setTable("sys_api")
                .setFields("path","name","description","pid")
                .setSearchFields("id")
                .insert();

        String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
        String Cusername = UserService.getCurrentUserName();
        String username = UserService.getCurrentUserAccount();
        String name = reqBody.get("name").toString();
        String ip = UserService.getCurrentUserIp();
        String content = "添加名称为："+name+"的接口";
        String type = "添加接口";
        LocalDateTime currentDateTime = LocalDateTime.now();
        String time = currentDateTime.toString();
        System.out.println(username);
        System.out.println(name);
        System.out.println(ip);
        System.out.println(content);
        System.out.println(type);
        simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");


        return new Result("ok", 200, ret);
    }

    // delete api
    @RequestMapping("/delete")
    public Result delete(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] apiIds = str.split(",");
        int rSet = 0;
        for (int i = 0; i < apiIds.length; i++) {
            Integer id = Integer.parseInt(apiIds[i]);
            rSet = simpleJDBC.update("delete from sys_api where id=?", id);

            //删除相关的表中数据
            simpleJDBC.update("delete from sys_role_api where api_id=?", id);
            simpleJDBC.update("delete from sys_menu_api where api_id=?", id);
        }

        String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
        String username = UserService.getCurrentUserAccount();
        String Cusername = UserService.getCurrentUserName();
        String name = reqBody.get("name").toString();
        String ip = UserService.getCurrentUserIp();
        String content = "删除名称为："+name+"的接口";
        String type = "删除接口";
        LocalDateTime currentDateTime = LocalDateTime.now();
        String time = currentDateTime.toString();
        System.out.println(username);
        System.out.println(name);
        System.out.println(ip);
        System.out.println(content);
        System.out.println(type);
        simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");

        return new Result("success", 200, rSet);
    }

    // update api
    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {
        if (reqBody.get("children") != null) {
            reqBody.remove("children");
        }
        // 一级菜单需将pid设为0
        if (reqBody.get("pid") == null) {
            reqBody.put("pid", 0);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
//        int ret = sqlUtil.setTable("sys_api")
//                .setFields("path","name","description","pid","sort","id")
//                .setSearchFields("id")
//                .update();
//
//        return new Result("ok", 200, ret);
        int rSet = new SqlUtil(reqBody)
                .setTable("sys_api")
                .setFields("path","name","description","pid","sort")
                .setWhere("id = ?",reqBody.get("id"))
                .update();

        String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
        String Cusername = UserService.getCurrentUserName();
        String username = UserService.getCurrentUserAccount();
        String name = reqBody.get("name").toString();
        String ip = UserService.getCurrentUserIp();
        String content = "编辑更改名称为："+name+"的接口";
        String type = "编辑更改接口";
        LocalDateTime currentDateTime = LocalDateTime.now();
        String time = currentDateTime.toString();
        System.out.println(username);
        System.out.println(name);
        System.out.println(ip);
        System.out.println(content);
        System.out.println(type);
        simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");

        return new Result(ResultCode.SUCCESS, rSet);
    }
}
