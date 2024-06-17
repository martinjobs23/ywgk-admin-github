package com.ceit.order;

import com.alibaba.fastjson2.JSONArray;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import com.ceit.workstation.service.WorkstationDistributionService;
import com.ceit.workstation.util.MessageUtil;
import com.ceit.workstation.util.OrderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller("/order/permission")
public class OrderPermissionController {


    @Autowired
    //private WorkstationManegementService workstationManegementService;
    private WorkstationDistributionService workstationDistributionService;
    private static final Logger logger = LoggerFactory.getLogger(OrderTaskController.class);

    //  查询 我的审批任务
    @RequestMapping("/getOrderList")
    public Result getDeviceBlackList(Map<String, Object> reqBody){
        String searchFiledName = "order_name,deviceObject,deviceListName,deviceList,order_num,start_time,end_time,status";
        String selectFieldNames = "bss_order.*";
        String[] optionNames ={"order_name,deviceObject,deviceListName,deviceList,order_num,start_time,end_time,status,spe_user"};

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setSearchFields(searchFiledName)
                .setOrderBy( "status desc,end_time desc , start_time desc")
                .setWhere("status = 4")
//                .setWhere("operatorlist like ?","%崔文超%")
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();
    }

    //许可工作票
    @RequestMapping("/permissionOrder")
    public Result orderSave(Map<String, Object> reqBody) {
        int result = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setFields("status")
                .setWhere("order_id = ?", reqBody.get("order_id"))
                .update();
        if (result >= 1) {
            return new Result(200, "工作票已许可", "success");
        }
        return new Result(200, "许可失败", "error");
    }

    //驳回待许可工作票
    @RequestMapping("/refuseOrder")
    public Result refuseOrder(Map<String, Object> reqBody) {
        int result = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setFields("status")
                .setWhere("order_id = ?", reqBody.get("order_id"))
                .update();
        if (result >= 1) {
            return new Result(200, "工作票已驳回", "success");
        }
        return new Result(200, "驳回失败", "error");
    }

    //    将object类型数据转为字符串
    public String objectToString(Object object) {
        JSONArray jsonArray = (JSONArray) object;
        String result = "";
        for (Object o :
                jsonArray) {
            result = result + o.toString() + ",";
        }
        result = result.substring(0, result.length() - 1);
        return result;
    }

}
