package com.ceit.admin.controller;

import com.alibaba.fastjson2.JSONObject;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller("/menuAuth")
public class MenuAuthController {
    @Autowired
    private SimpleJDBC simpleJDBC;

    private final String tableName = "sys_role_menu";
    private final String[] selectFieldNames = { "menu_id" };
    private final String[] searchFiledNames = { "role_id" };


    @RequestMapping("/list")
    public Result list(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        SqlUtil.changeSearchFieldName(reqBody, "roleId", "role_id");

        sqlUtil.setTable(tableName)
                .setSearchFields(searchFiledNames)
                .setFields(selectFieldNames);
        return new Result(200,"ok",sqlUtil.selectForMapList());
    }


    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {

        String roleId = reqBody.get("roleId").toString();
        String str = reqBody.get("menuIds").toString();

        String getStr = str.substring(str.indexOf("[") + 1, str.indexOf("]"));
        String[] menuIds = getStr.split(",");

        String selectSql = "select * from sys_role_menu where role_id = ? ";
        simpleJDBC.update("delete from sys_role_menu where role_id=?", roleId);

        int rSet = 0;
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        reqBody.put("role_id",roleId);
        reqBody.put("menu_id", Arrays.asList(menuIds));
        rSet = sqlUtil.setTable("sys_role_menu")
                .setFields("role_id","menu_id")
                .insert();
        return new Result("ok", 200, rSet);
    }

}
