package com.ceit.device;

import java.util.*;

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

@Controller("/dgroup")
public class DGroupController {

    @Autowired
    private SimpleJDBC simpleJDBC;

    @Autowired
    private JSON json;

    @RequestMapping("/navigate")
    public Result navigate(Map<String, Object> reqBody) throws Exception{
        String sql;
        String[] selectFieldNames = { "*" };
        String selectTableName = "ywsj_dgroup";
        String[] groupBy = null;
        String orderBy = "sort,pid";
        String[] searchFiledName = { "id" };
        String[] acceptOptionNames = { "name" };

        List<Object> sqlParamObjList = new ArrayList<Object>();

        sql = SearchOptionPageUtil.getSelectSql(selectTableName, selectFieldNames, null, searchFiledName,
                acceptOptionNames, groupBy, orderBy, reqBody, sqlParamObjList);

        JSONArray jsonData = simpleJDBC.selectForJsonArray(sql, sqlParamObjList.toArray());

        return new Result(ResultCode.SUCCESS_TOTREE, jsonData);

    }

    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) throws Exception{    
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int rSet = sqlUtil.setTable("ywsj_dgroup")
                .setFields("pid", "name", "sort","id", "description")
                .insert();
        return new Result("success", 200, rSet);
    }

    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) throws Exception {
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int rSet = sqlUtil.setTable("ywsj_dgroup")
                .setFields("pid","name","description","sort")
                .setWhere("id = ?",reqBody.get("id"))
                .update();
        return new Result("ok",200,rSet);
    }

    @RequestMapping("/delete")
    public Result delete(Map<String,Object> reqBody) throws Exception {

        String str = reqBody.get("id").toString();
        String[] groupIds = str.split(",");
        //int rSet = 0;
        //删除该设备组
        StringBuilder sql = new StringBuilder();
        sql.append("delete from ywsj_dgroup where id in (");
        for (int j = 0;j< groupIds.length; j++) {
            if (j > 0)
                sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        int ret= simpleJDBC.update(sql.toString(),groupIds);
        //删除该角色对应的设备组权限
        // if (ret!=0) {
        //     for(int i = 0;i< groupIds.length;i++){
        //         rSet = simpleJDBC.update("delete from sys_role_dgroup where role_id=? and dgroup_id =?",reqBody.get("roleId"),groupIds[i]);
        //     }
        // }
        return new Result("ok",200,ret);
    }
 }
