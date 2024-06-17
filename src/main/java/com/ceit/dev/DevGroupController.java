package com.ceit.dev;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.json.JSON;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;

@Controller("/dev/group")
public class DevGroupController {

    @Autowired
    private SimpleJDBC simpleJDBC;

    @Autowired
    private JSON json;

    private Result selectJsonData(Map<String, Object> reqBody, boolean singleObject) {
        String[] selectFieldNames = {"*"};
        String selectTableName = "dev_group";
        String whereCondition = null;
        String[] searchFiledNames = {"id"};
        String[] acceptOptionNames = {"name", "phone_number"};
        String orderBy = null;
        List<Object> sqlParamObjList = new ArrayList<Object>();

        if (singleObject) {
            // 单条数据, 查询条件添加 limit 1
            reqBody.put("pageNow", 1);
            reqBody.put("pageSize", 1);
            orderBy = null;
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);

        if (singleObject) {
            JSONObject jsonData = sqlUtil.setTable(selectTableName).setFields(selectFieldNames).setAcceptOptions(acceptOptionNames).setSearchFields(searchFiledNames).setWhere("status = 0").selectForJsonObject();
            return new Result(200, "ok", jsonData);
        } else {
            List list = sqlUtil.setTable(selectTableName).setFields(selectFieldNames).setAcceptOptions(acceptOptionNames).setSearchFields(searchFiledNames).setWhere("status = 0").selectForMapList();
            return new Result(200, "ok", list);
        }
    }

    /**
     * 树形展示数组
     */
    @RequestMapping("/tree")
    public Result treeData(Map<String, Object> reqBody) {
        Result result = selectJsonData(reqBody, false);
        result.setCode(201);
        return result;
    }

    // insert organization
    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {
        if (reqBody.get("children") != null) {
            reqBody.remove("children");
        }
        // 添加一级组织需将pid设为0
        if (reqBody.get("pid") == null) {
            reqBody.put("pid", 0);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("dev_group").setFields("pid", "name", "sort", "user_id", "phone_number", "description").insert();
        return new Result("success", 200, ret);
    }

    // update organization
    @RequestMapping("/update")
    public Result update(Map<String, Object> reqBody) {
        if (reqBody.get("children") != null) {
            reqBody.remove("children");
        }
        // 添加一级组织需将pid设为0
        if (reqBody.get("pid") == null) {
            reqBody.put("pid", 0);
        }
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("dev_group").setFields("pid", "name", "sort", "user_id", "phone_number", "description").setWhere("id = ?", reqBody.get("id")).update();
        return new Result("success", 200, ret);
    }

    // delete organization
    @RequestMapping("/delete")
    public Result deleteById(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] orgIds = str.split(",");
        //如果存在终端的org_id与要被删除的id相同，则不允许删除
        int ifdelete = 0;
        for (int i = 0; i < orgIds.length; i++) {
            Integer id = Integer.parseInt(orgIds[i]);
            ifdelete = simpleJDBC.selectForOneInt("select count(*) from dev_info where group_id=?", id);
            if (ifdelete != 0) {
                return new Result("分组下有设备，不允许删除", 200, 0);
            }
        }
        int rSet = 0;
        for (int i = 0; i < orgIds.length; i++) {
            Integer id = Integer.parseInt(orgIds[i]);
            rSet = simpleJDBC.update("update dev_group set status = 1 where id=?", id);
        }
        return new Result("success", 200, rSet);
    }

    // update sort
    @RequestMapping("/sort")
    public Result sort(Map<String, Object> reqBody) {
        int rSet = simpleJDBC.update("update dev_group set sort=? where id=?", reqBody.get("sort"), reqBody.get("id"));
        return new Result(ResultCode.SUCCESS, rSet);
    }

}
