package com.ceit.admin.controller;

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

@Controller("/org")
public class OrganizationController {

    @Autowired
    private SimpleJDBC simpleJDBC;

    @Autowired
    private JSON json;

    private Result selectJsonData(Map<String, Object> reqBody, boolean singleObject) {
        String[] selectFieldNames = { "*" };
        String selectTableName = "sys_organization";
        String whereCondition = null;
        String[] searchFiledNames = { "id" };
        String[] acceptOptionNames = { "name", "code", "e_name", "shortname", "phone_number"};
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
            JSONObject jsonData= sqlUtil.setTable(selectTableName)
                    .setFields(selectFieldNames)
                    .setAcceptOptions(acceptOptionNames)
                    .setSearchFields(searchFiledNames)
                    .selectForJsonObject();
            return new Result(201, "ok",jsonData);
        }
        else{
            List list = sqlUtil.setTable(selectTableName)
                    .setFields(selectFieldNames)
                    .setAcceptOptions(acceptOptionNames)
                    .setSearchFields(searchFiledNames)
                    .selectForMapList();

            return new Result(201, "ok",list);
        }
    }

    private Integer selectTotalCount(Map<String, Object> reqBody) {

        String sql;

        String[] selectFieldNames = { "count(*)" };
        String selectTableName = "sys_organization";
        String whereCondition = null;
        String[] searchFiledNames = { "org_id" };
        String[] acceptOptionNames = { "name", "title", "email", "tel" };
        String orderBy = null;
        List<Object> sqlParamObjList = new ArrayList<Object>();

        // orgId改成org_id
        SqlUtil.changeSearchFieldName(reqBody, "orgId", "org_id");

        // 删除 limit
        reqBody.remove("pageNow");
        reqBody.remove("pageSize");

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        Object count = sqlUtil.setTable(selectTableName)
                .setFields(selectFieldNames)
                .setAcceptOptions(acceptOptionNames)
                .setSearchFields(searchFiledNames)
                .selectForOneNode();
        if (count == null)
            return -1;
        else
            return (Integer) count;
    }

    /**
     * 树形展示数组
     */
    @RequestMapping("/tree")
    public Result treeData(Map<String, Object> reqBody) {

        return selectJsonData(reqBody, false);

    }

    /**
     * 不分页列表数组
     */
    @RequestMapping("/list")
    public Result listData(Map<String, Object> reqBody) {

        return selectJsonData(reqBody, false);
    }

    /**
     * 分页列表数组
     */
    @RequestMapping("/page")
    public Result pageData(Map<String, Object> reqBody) {

        Result result =  selectJsonData(reqBody, false);

        // selectTotalCount 会修改reqBody值，要放最后调用
        int totalcount = selectTotalCount(reqBody);

        if (totalcount < 0) {
            return new Result("Get Total Count Failed", 500, result.getData());
        }

        // FIXME： 把客户端需要分页的数据标准化

        return new Result("ok", 200, result.getData());
    }

    /**
     * 单个对象数据
     */
    @RequestMapping("/get")
    public Result getObject(Map<String, Object> reqBody) {

        return selectJsonData(reqBody, true);
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
        int ret = sqlUtil.setTable("sys_organization")
                .setFields("pid","code","name","sort","e_name","shortname","logo","phone_number","fax","description")
                .insert();
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
        int ret = sqlUtil.setTable("sys_organization")
                .setFields("pid","code","name","sort","e_name","shortname","logo","phone_number","fax","description")
                .setWhere("id = ?",reqBody.get("id"))
                .update();
        return new Result("success", 200, ret);
    }

    // delete organization
    @RequestMapping("/delete")
    public Result deleteById(Map<String, Object> reqBody) {
        String str = reqBody.get("id").toString();
        String[] orgIds = str.split(",");
        //如果存在终端的org_id与要被删除的id相同，则不允许删除
        int ifdelete = 0;
        for (int i = 0; i < orgIds.length; i++){
            Integer id = Integer.parseInt(orgIds[i]);
            ifdelete = simpleJDBC.selectForOneInt("select count(*) from dev_cert where org_id=?", id);
            if(ifdelete != 0){
                return new Result("分组下有终端，不允许删除",200, 0 );
            }
        }
        int rSet = 0;
        for (int i = 0; i < orgIds.length; i++) {
            Integer id = Integer.parseInt(orgIds[i]);
            rSet = simpleJDBC.update("delete from sys_organization where id=?", id);
        }
        return new Result("success", 200, rSet);
    }

    // update sort
    @RequestMapping("/sort")
    public Result sort(Map<String, Object> reqBody) {

        int rSet = simpleJDBC.update(
            "update sys_organization set sort=? where id=?", 
            reqBody.get("sort"),
            reqBody.get("id")
        );
        return new Result(ResultCode.SUCCESS, rSet);
    }        

}
