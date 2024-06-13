package com.ceit.admin.controller;

import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller("/apiAuth")
public class ApiAuthController {
    @Autowired
    private SimpleJDBC simpleJDBC;

    private final String tableName = "sys_role_api";
    private final String[] selectFieldNames = { "api_id" };
    private final String[] searchFiledNames = { "role_id" };


    @RequestMapping("/list")
    public Result list(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        SqlUtil.changeSearchFieldName(reqBody, "roleId", "role_id");
        List list = sqlUtil.setTable(tableName)
                .setSearchFields(searchFiledNames)
                .setFields(selectFieldNames)
                .selectForMapList();
        return new Result("ok", 200, list);
    }


    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody, HttpServletRequest request) {

        String roleId = reqBody.get("role_id").toString();
        String str = reqBody.get("api_ids").toString();

        String getStr = str.substring(str.indexOf("[") + 1, str.indexOf("]"));
        String[] apiIds = getStr.split(",");

        simpleJDBC.update("delete from sys_role_api where role_id=?", roleId);

        int rSet = 0;
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        reqBody.put("role_id",roleId);
        reqBody.put("api_id", Arrays.asList(apiIds));
        rSet = sqlUtil.setTable("sys_role_api")
                .setFields("role_id","api_id")
                .insert();

        String audit = "insert into sercuityAudit (Cusername,username,ip,content,type,time,level) value (?,?,?,?,?,?,?)";
        String Cusername = UserService.getCurrentUserName();
        String username = UserService.getCurrentUserAccount();
        String role_name = reqBody.get("role_name").toString();
        String api_name= reqBody.get("api_name").toString();
        String ip = UserService.getCurrentUserIp();
        String type = "授权接口";
        LocalDateTime currentDateTime = LocalDateTime.now();
        String time = currentDateTime.toString();
        String[] tokens = role_name.split(",");
        for(String token:tokens)
        {
            String content = "授权名称："+token+"的接口为"+api_name;
            simpleJDBC.update(audit,Cusername,username,ip,content,type,time,"中");
            System.out.println(username);
            System.out.println(role_name);
            System.out.println(api_name);
            System.out.println(ip);
            System.out.println(content);
            System.out.println(type);
        }
        return new Result("ok", 200, rSet);
    }

}
