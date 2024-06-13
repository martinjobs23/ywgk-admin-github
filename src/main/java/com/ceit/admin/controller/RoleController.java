package com.ceit.admin.controller;

import com.alibaba.fastjson2.JSONArray;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ceit.utils.SearchOptionPageUtil;
import com.ceit.utils.SqlUtil;

@Controller("/role")
public class RoleController {
    @Autowired
    private SimpleJDBC simpleJDBC;
    // 查询左侧的树形结构
    @RequestMapping("/navigate")
    public Result navigate(Map<String, Object> reqBody) {
        String roleSql = "select * from sys_role s ";
        JSONArray tmp = simpleJDBC.selectForJsonArray(roleSql);
        return new Result(ResultCode.SUCCESS_TOTREE, tmp);
    }

    //获取右侧角色--策略列表
    @RequestMapping("/getRoleAuthPolicy")
    public Result getRoleAuthPolicy(Map<String, Object> reqBody) {

        String sql;
        List<Object> sqlParamObjList = new ArrayList<Object>();

        String selectTableName = " sys_role s, sys_auth_policy ap";
        String[] selectFieldNames = { "distinct s.*", "ap.name as auth_policy_name" };
        String[] whereConditonion = {"s.auth_policy_id = ap.id"};
        String[] searchFiledNames = null;
        String[] acceptOptionNames = { "s.name", "ap.name" };
        String[] groupBy = null;
        String orderBy = null;

        // 多表查询，name字段冲突，修改字段名称
        SqlUtil.changeOptionFieldName(reqBody, "name", "s.name");
        SqlUtil.changeOptionFieldName(reqBody, "auth_policy_name", "ap.name");

        sql = SearchOptionPageUtil.getSelectSql(selectTableName, selectFieldNames, whereConditonion, searchFiledNames,
                acceptOptionNames, groupBy, orderBy, reqBody, sqlParamObjList);

        JSONArray jsonData = simpleJDBC.selectForJsonArray(sql, sqlParamObjList.toArray());
        return new Result(ResultCode.SUCCESS_TOTREE, jsonData);
    }

    // 获取认证策略列表
    @RequestMapping("/getAuthPolicyList")
    public Result getAuthPolicyList(Map<String, Object> reqBody) {
        String authPolicySql = "select * from sys_auth_policy s ";
        JSONArray tmp = simpleJDBC.selectForJsonArray(authPolicySql);
        return new Result(ResultCode.SUCCESS_TOTREE, tmp);
    }

    // 角色插入
    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {
        if (reqBody.get("children") != null) {
            reqBody.remove("children");
        }
        if (reqBody.get("pid") == null) {
            reqBody.put("pid", 0);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("sys_role")
                .setFields("pid","auth_policy_id","name")
                .insert();
        return new Result("success", 200, ret);
    }

    @RequestMapping("/delete")
    public Result deleteByIds(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] roleIds = str.split(",");
        int rSet = 0;

        StringBuilder sql = new StringBuilder();
        sql.append("delete from sys_role where id in (");
        for (int i = 0; i < roleIds.length; i++) {
            if (i > 0)
                sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        rSet = simpleJDBC.update(sql.toString(), roleIds);
        return new Result("success", 200, rSet);

    }

    // 角色修改
    @RequestMapping("/edit")
    public Result edit(Map<String, Object> reqBody) {
        if (reqBody.get("children") != null) {
            reqBody.remove("children");
        }
        if (reqBody.get("pid") == null) {
            reqBody.put("pid", 0);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("sys_role")
                .setFields("pid","auth_policy_id","name")
                .setWhere("id = ?",reqBody.get("id"))
                .update();
        return new Result("success", 200, ret);
    }

}
