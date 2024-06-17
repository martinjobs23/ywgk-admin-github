package com.ceit.order;

import com.alibaba.fastjson2.JSONArray;
import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import com.ceit.workstation.controller.WorkstationDistributionController;
import com.ceit.workstation.service.WorkstationDistributionService;
import com.ceit.workstation.util.MessageUtil;
import com.ceit.workstation.util.OrderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller("/order/pass")
public class OrderPassController {


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
                .setWhere("status = 3")
//                .setWhere("operatorlist like ?","%崔文超%")
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();
    }

    //审批通过工作票
    @RequestMapping("/passOrder")
    public Result orderSave(Map<String, Object> reqBody) {
        Result result2 = resourceAllocation(reqBody);
        if(result2.getMsg().equals("success")){
            int result = new SqlUtil(reqBody)
                    .setTable("bss_order")
                    .setFields("status")
                    .setWhere("order_id = ?", reqBody.get("order_id"))
                    .update();
            reqBody.put("spe_status",1);
            reqBody.put("spe_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            int result1 = new SqlUtil(reqBody)
                    .setTable("audit_bss_order")
                    .setFields("spe_status,spe_time")
                    .setWhere("order_id = ? AND speUserId = ?", reqBody.get("order_id"), UserService.getCurrentUserId())
                    .update();
            if (result*result1 >= 1) {
                return new Result(200, "审批成功", "success");
            }
        }
        return result2;
    }

    //驳回待许可工作票
    @RequestMapping("/refuseOrder")
    public Result refuseOrder(Map<String, Object> reqBody) {
        int result = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setFields("status")
                .setWhere("order_id = ?", reqBody.get("order_id"))
                .update();
        reqBody.put("spe_status",2);int result1 = new SqlUtil(reqBody)
                .setTable("audit_bss_order")
                .setFields("spe_status")
                .setWhere("order_id = ? & speUserId = ?", reqBody.get("order_id"), UserService.getCurrentUserId())
                .update();
        if (result*result1 >= 1) {
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

    //    资源分配
    public Result resourceAllocation(Map<String, Object> reqBody) {
        List<Map<String, Object>> maps = new SqlUtil(reqBody)
                .setWhere("order_id=?", reqBody.get("order_id").toString())
                .setTable("order_operators")
                .setFields("USER_ID")
                .setAcceptOptions("USER_ID")
                .selectForMapList();
        ArrayList list=new ArrayList();
        for(int i=0;i<maps.size();i++){
            HashMap hashMap = (HashMap) maps.get(i);
            OrderUtil orderUtil = new OrderUtil();
            orderUtil.setOperator_id(String.valueOf(hashMap.get("USER_ID")));
            //orderUtil.setOperator_name((String) hashMap.get("USER_ID"));
            list.add(orderUtil);
        }
//        messageUtil.setOperatorList(data.get(0));
        MessageUtil messageUtil=new MessageUtil();
        messageUtil.setOperatorList(list);
        messageUtil.setOrder_id(reqBody.get("order_id").toString());
        messageUtil.setLocation(reqBody.get("location").toString());
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 将字符串转换为LocalDateTime对象
        LocalDateTime start_time = LocalDateTime.parse(reqBody.get("start_time").toString(), formatter);
        LocalDateTime end_time = LocalDateTime.parse(reqBody.get("end_time").toString(), formatter);
        messageUtil.setOperator_start_time(start_time);
        messageUtil.setOperator_end_time(end_time);

//      分配工位
        Result result=workstationDistributionService.orderDistribute(messageUtil);


        return result;
    }
}
