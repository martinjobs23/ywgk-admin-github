package com.ceit.device;

import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller("/dgroupAuth")
public class DGroupAuthController {
    @Autowired
    private SimpleJDBC simpleJDBC;

    private final String tableName = "sys_role_dgroup";
    private final String[] selectFieldNames = { "dgroup_id" };
    private final String[] searchFiledNames = { "role_id" };


    @RequestMapping("/list")
    public Result list(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        SqlUtil.changeSearchFieldName(reqBody, "roleId", "role_id");
        List jsonData = sqlUtil.setTable(tableName)
                .setSearchFields(searchFiledNames)
                .setFields(selectFieldNames)
                .selectForMapList();
        return new Result("ok", 200, jsonData);
    }


    @RequestMapping("/insert")
    public Result insert(Map<String, Object> reqBody) {

        String roleId = reqBody.get("roleId").toString();
        String str = reqBody.get("groupIds").toString();

        String getStr = str.substring(str.indexOf("[") + 1, str.indexOf("]"));
        String[] groupIds = getStr.split(",");

        String selectSql = "select * from sys_role_dgroup where role_id = ? ";

        simpleJDBC.update("delete from sys_role_dgroup where role_id=?", roleId);

        int rSet = 0;
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        reqBody.put("role_id",roleId);
        reqBody.put("dgroup_id", Arrays.asList(groupIds));
        rSet = sqlUtil.setTable("sys_role_dgroup")
                .setFields("role_id","dgroup_id")
                .insert();
        return new Result("ok", 200, rSet);
    }

}
