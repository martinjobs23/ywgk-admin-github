package com.ceit.strategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.json.JSON;
import com.ceit.response.Result;
import com.ceit.response.ResultCode;
import com.ceit.utils.SqlUtil;

import javax.servlet.http.HttpServletRequest;


@Controller("/strategy/pattern")
public class pattern {
    @Autowired
    private SimpleJDBC simpleJDBC;

    @Autowired
    private JSON json;



    @RequestMapping("/page")
    public Result page(Map<String, Object> reqBody, HttpServletRequest request) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        String tableName = "business_pattern";
        String[] optionNames = {"id", "inspection_ticket", "ban_order", "time_strategy", "operate_location","audit_pattern","inform_path"};
        //类型处理
        Object typeId = reqBody.get("type_id");
        if (typeId != null && !typeId.toString().trim().equals("")) sqlUtil.setSearchFields("type_id");

        //过滤数据权限,查询条件中添加org_id
        sqlUtil.setTable(tableName)
                .setAcceptOptions(optionNames);
        Result object = sqlUtil.selectForTotalRowsResult();
        return sqlUtil.selectForTotalRowsResult();
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
        System.out.println("1111111111111111111111111111111111111111111111111");
        System.out.println(reqBody);
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        int ret = sqlUtil.setTable("business_pattern").setFields("inspection_ticket", "operation_ticket", "sys_ticket", "construction_ticket").insert();
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
        int ret = sqlUtil.setTable("business_pattern").setFields("inspection_ticket", "operation_ticket", "sys_ticket", "construction_ticket").setWhere("id = ?", reqBody.get("id")).update();
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
            ifdelete = simpleJDBC.selectForOneInt("select count(*) from business_pattern where id=?", id);
            if (ifdelete != 0) {
                return new Result("分组下有设备，不允许删除", 200, 0);
            }
        }
        int rSet = 0;
        for (int i = 0; i < orgIds.length; i++) {
            Integer id = Integer.parseInt(orgIds[i]);
            rSet = simpleJDBC.update("update business_pattern  where id=?", id);
        }
        return new Result("success", 200, rSet);
    }
}
