package com.ceit.admin.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONArray;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.json.JSON;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SearchOptionPageUtil;
import com.ceit.utils.SqlUtil;

@Controller("/menu")
public class MenuController {

    @Autowired
    private SimpleJDBC simpleJDBC;

    @Autowired
    private JSON json;

    @RequestMapping("/getPermissions")
    public Result getPermissions(Map<String, Object> reqBody) {

        //所有用户都要返回 / 和 404 前端路由，否则登录后前端找不到路由        
        Integer userId = (Integer) reqBody.get("userId");
        String sql = "select *, '' as children from sys_menu t WHERE "+
                "path='/' or path='/index' or path='/401' or path='/404' or " +
                "t.id in (SELECT menu_id FROM sys_role_menu WHERE role_id IN" +
                "(SELECT role_id FROM sys_role_user WHERE user_id = ?))";

        return new Result(ResultCode.SUCCESS_TOTREE,
                simpleJDBC.selectForJsonArray(sql, userId));

    }

    @RequestMapping("/navigate")
    public Result navigate(Map<String, Object> reqBody) {

        String sql;
        String[] selectFieldNames = { "*" };
        String selectTableName = "sys_menu";
        String[] groupBy = null;
        String orderBy = "sort,pid";
        String[] searchFiledNames = { "id" };
        String[] acceptOptionNames = { "name", "title", "path", "redirect", "component" };

        List<Object> sqlParamObjList = new ArrayList<Object>();

        sql = SearchOptionPageUtil.getSelectSql(selectTableName, selectFieldNames, null, searchFiledNames,
                acceptOptionNames, groupBy, orderBy, reqBody, sqlParamObjList);

        System.out.println("menulist sql = " + sql);
        System.out.println("menulist sqlParamObjList.len = " + sqlParamObjList.size());
        System.out.println("menulist sqlParamObjList = " + sqlParamObjList);

        JSONArray data = simpleJDBC.selectForJsonArray(sql, sqlParamObjList.toArray());

        return new Result(ResultCode.SUCCESS_TOTREE, data);

    }

    @RequestMapping("/getIdByRoleId")
    public Result list(Map<String, Object> reqBody) throws Exception {

        String sql = "select menu_id from sys_role_menu where role_id=?";

        JSONArray jsonData = simpleJDBC.selectForJsonArray(sql, reqBody.get("roleId"));

        return new Result(ResultCode.SUCCESS, jsonData);
    }

    // insert menu
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
        int rSet = sqlUtil.setTable("sys_menu")
                .setFields("pid", "sort", "name", "title", "path", "redirect", "component", "icon", "badge", "hidden",
                        "alwaysShow", "description", "disabled")
                .insert();
        return new Result("success", 200, rSet);
    }

    // delete menu
    @RequestMapping("/delete")
    public Result delete(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] menuIds = str.split(",");
        int rSet = 0;
        for (int i = 0; i < menuIds.length; i++) {
            Integer id = Integer.parseInt(menuIds[i]);
            rSet = simpleJDBC.update("delete from sys_menu where id=?", id);

            //删除相关的表中数据
            simpleJDBC.update("delete from sys_role_menu where menu_id=?", id);
            simpleJDBC.update("delete from sys_menu_api where menu_id=?", id);
        }
        return new Result("success", 200, rSet);
    }

    // update menu
    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {

        if (reqBody.get("children") != null) {
            reqBody.remove("children");
        }
        // 一级菜单需将pid设为0
        if (reqBody.get("pid") == null) {
            reqBody.put("pid", 0);
        }
        int rSet = new SqlUtil(reqBody)
                .setTable("sys_menu")
                .setFields("pid", "sort", "name", "title", "path", "redirect", "component", "icon", "badge", "hidden",
                        "alwaysShow", "description", "disabled")
                .setWhere("id = ?",reqBody.get("id"))
                .update();
        return new Result(ResultCode.SUCCESS, rSet);
    }

        // update sort
        @RequestMapping("/sort")
        public Result sort(Map<String, Object> reqBody) {
 
            // 一级菜单需将pid设为0
            int rSet = simpleJDBC.update(
                "update sys_menu set sort=? where id=?", 
                reqBody.get("sort"),
                reqBody.get("id")
            );
            return new Result(ResultCode.SUCCESS, rSet);
        }
}
