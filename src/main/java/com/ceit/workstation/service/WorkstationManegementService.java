package com.ceit.workstation.service;

import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Component;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import com.ceit.workstation.grpc.ZjWebAdminClient;

import java.util.Map;

@Component
public class WorkstationManegementService {

    @Autowired
    SimpleJDBC simpleJDBC;

    //获取已注册终端列表
    public Result getCheckList(Map<String, Object> reqBody){
        //条件查询
        String[] optionNames ={"name,ip,mac,location"};
//        String selectFieldNames = "dev_cert.*,sys_organization.name,sys_user.name as user_name,last_online_time";
        String selectFieldNames = "workstation_info.*";
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        ZjWebAdminClient zjWebAdminClient = new ZjWebAdminClient();
        Result R = zjWebAdminClient.getAccountList(205);
        Result jsonData = sqlUtil
                //.setTable("dev_cert left join sys_organization on dev_cert.org_id = sys_organization.id left join sys_user on dev_cert.user_id = sys_user.id")
                .setTable("workstation_info")
                .setAcceptOptions(optionNames)
                .setFields(selectFieldNames)
                .setOrderBy("online desc, online_time desc")
                .selectForTotalRowsResult();
//        int count = sqlUtil.selectForTotalCount();
//        jsonData = "{\"totalCount\":" + count + ",\"jsonData\":" + jsonData + "}";
        return jsonData;
    }
    public Result getOrderList(Map<String, Object> reqBody){
        //条件查询
        System.out.println("pageNow" + reqBody.get("pageNow"));
        System.out.println("pageSize" + reqBody.get("pageSize"));
        String selectFieldNames = "workstation_order.*";
        //条件查询
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        Result jsonData = sqlUtil.setTable("workstation_order")
                .setWhere("workstation_id=?",reqBody.get("workstation_id"))
                .setFields(selectFieldNames)
                .setOrderBy("time desc")
                .selectForTotalRowsResult();
//        int count = sqlUtil.selectForTotalCount();
//        jsonData = "{\"totalCount\":" + count + ",\"jsonData\":" + jsonData + "}";
        return jsonData;
    }
    //获取终端状态
    public Result workstationStatus(Map<String, Object> reqBody){
        String selectFieldNames = "workstation_info.*";
        String searchFiledName = "id";

        System.out.println(reqBody);
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        Result jsonData = sqlUtil.setTable("workstation_info")
                .setFields(selectFieldNames)
                .setSearchFields(searchFiledName)
                //.setWhere("time = (SELECT MAX( time ) FROM dev_status d2" + " where d1.dev_ip = d2.dev_ip)")
                .selectForTotalRowsResult();
//        jsonData = "{\"jsonData\":" + jsonData + "}";
        System.out.println(jsonData);
        return jsonData;
    }

}
