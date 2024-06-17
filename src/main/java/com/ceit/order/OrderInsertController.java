package com.ceit.order;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ceit.admin.service.UserService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller("/order/insert")
public class OrderInsertController {


    private static final Logger logger = LoggerFactory.getLogger(OrderTaskController.class);

    //工作票暂存草稿；提交审核
    @RequestMapping("/orderSave")
    public Result orderSave(Map<String, Object> reqBody) {
        String order_num = GetNewOrderNum();
//        分离开始时间结束时间，并更改格式
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JSONArray startEndTime = (JSONArray) reqBody.get("start_end_time");
        Date start = startEndTime.getDate(0);
        Date end = startEndTime.getDate(1);
        String start_time = simpleDateFormat.format(start);
        String end_time = simpleDateFormat.format(end);
        String create_time = simpleDateFormat.format(new Date());

//        设备Id列表,需存入设备工作票对应表
        JSONArray deviceListId = (JSONArray) reqBody.get("deviceListId");
//        审核人员Id列表,需存入审核人员工作票对应表
        JSONArray speUserId = (JSONArray) reqBody.get("speUserId");
//        运维人员Id列表,需存入运维人员工作票对应表
        JSONArray operatorListId = (JSONArray) reqBody.get("operatorListId");


//        放入requestbody
        reqBody.put("start_time", start_time);
        reqBody.put("end_time", end_time);
        reqBody.put("create_time", create_time);
        reqBody.put("lastmod_time", create_time);
        reqBody.put("creater_id", UserService.getCurrentUserId());
        reqBody.put("modifyer_id", UserService.getCurrentUserId());
        reqBody.put("creater_name", UserService.getCurrentUserName());
        reqBody.put("order_from", 2);

        Object object = reqBody.get("spe_user");
        reqBody.put("spe_user", objectToString(reqBody.get("spe_user")));
        reqBody.put("operatorList", objectToString(reqBody.get("operatorList")));
        reqBody.put("deviceListName", objectToString(reqBody.get("deviceListName")));
        reqBody.put("deviceList", objectToString(reqBody.get("deviceList")));
        reqBody.put("tqzh", objectToString(reqBody.get("tqzh")));
        reqBody.put("order_num", order_num);

//        将工作票存入数据库
//        sqlutil方法insertAutoIncKey返回值为新增数据项id
        SqlUtil sqlUtil = new SqlUtil(reqBody);
        Integer bss_order_result = sqlUtil
                .setTable("bss_order")
                .setFields("order_num", "user_name", "spe_user", "order_name", "order_type", "deviceList", "deviceListName", "start_time",
                        "end_time", "create_time", "description", "status", "lastmod_time", "creater_id", "location",
                        "modifyer_id", "deviceObjectId", "deviceObject", "operatorList", "order_from", "tqzh")
                .setSearchFields("order_id")
                .insertAutoIncKey();


//        将工作票对应运维设备存入数据库表 order_devices
//        此处deviceListId为JSONARRAY类型，若包含多个值，sqlutil可直接增加多条记录
        Map<String, Object> order_devices_map = new HashMap<>();
        order_devices_map.put("DEVICE_ID", deviceListId);
        order_devices_map.put("ORDER_ID", bss_order_result);
        String[] tqzh = reqBody.get("tqzh").toString().split(",");
        for (int i = 0; i < tqzh.length; i++) {
            if (tqzh[i].equals("null")) {
                tqzh[i] = "";
            }
        }
        order_devices_map.put("tqzh", tqzh);
        int order_devices_result = new SqlUtil(order_devices_map)
                .setTable("order_devices")
                .setFields("DEVICE_ID", "ORDER_ID", "tqzh")
                .insert();

//        将工作票对应运维人员存入数据库表 order_operators
//        此处operatorListId为JSONARRAY类型，若包含多个值，sqlutil可直接增加多条记录
        Map<String, Object> order_operators_map = new HashMap<>();
        order_operators_map.put("USER_ID", reqBody.get("operatorListId"));
        order_operators_map.put("ORDER_ID", bss_order_result);
        int order_operators_result = new SqlUtil(order_operators_map)
                .setTable("order_operators")
                .setFields("USER_ID", "ORDER_ID")
                .insert();

        reqBody.put("order_id", bss_order_result);
        reqBody.put("spe_user", object);
        int audit_bss_order_result = new SqlUtil(reqBody)
                .setTable("audit_bss_order")
                .setFields("order_id", "order_name", "speUserId", "spe_user", "spe_status", "start_time", "end_time", "create_time", "creater_name", "creater_id")
                .insert();

        int result = bss_order_result * order_devices_result * order_operators_result * audit_bss_order_result;
        if (result >= 1) {
            return new Result(200, "success", 1);
        }
//        如果插入数据库错误，返回每个表的插入结果
        return new Result(200, "error", new HashMap<String, Integer>() {
            {
                put("bss_order_result", bss_order_result);
                put("order_devices_result", order_devices_result);
                put("order_operators_result", order_operators_result);
                put("audit_bss_order_result", audit_bss_order_result);
            }
        });
    }

    //    将object类型数据转为字符串
    public String objectToString(Object object) {
        JSONArray jsonArray = (JSONArray) object;
        String result = "";
        for (Object o :
                jsonArray) {
            if (o == null)
                o = "null";
            result = result + o.toString() + ",";
        }
        result = result.substring(0, result.length() - 1);
        return result;
    }


    // 为工作票自动生成一个唯一的Id,格式化
    public static String GetNewOrderNum() {

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String today = dateFormat.format(date);

        int todayNum = new SqlUtil(new HashMap<>())
                .setTable("bss_order")
                .setWhere("create_time>= ?", today)
                .selectForTotalCount();

        /** id自增,根据今日新增的工作票数量得出id
         *  id格式：4位，如0001、2334
         */
        String idString = String.format("%04d", todayNum);
        return "HN" + today + idString;
    }

}
