package com.ceit.order;

import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

//import com.ceit.ioc.annotations.Autowired;
//import com.ceit.ioc.annotations.Controller;
//import com.ceit.ioc.annotations.RequestMapping;
//import com.ceit.jdbc.SimpleJDBC;
//import com.ceit.response.Result;
//import com.ceit.utils.SqlUtil;
//
//import javax.servlet.http.HttpServletRequest;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
@Controller("/order/manage")
public class OrderManageController {

    @Autowired
    private SimpleJDBC simpleJDBC;
    private static final Logger logger= LoggerFactory.getLogger(OrderManageController.class);

    //查询 工作票内容

    @RequestMapping("/getTicketList")
    public Result GetTicketList(Map<String, Object> reqBody) {
        String selectFieldNames = "bss_order.*";
        String[] optionNames = {"order_name,deviceList,order_num,start_time,end_time,status,spe_user"};

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setOrderBy("end_time desc , start_time desc")
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();

    }
    //查询 草稿工作票内容
    @RequestMapping("/getDraftTicketList")
    public Result getDraftTicketList(Map<String, Object> reqBody) {
        String selectFieldNames = "bss_order.*";
        String[] optionNames = {"order_name,deviceList,order_num,start_time,end_time,status,spe_user"};

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setOrderBy("end_time desc , start_time desc")
                .setWhere("status IN (0,1,2)")
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();

    }

    //查询 待审核工作票内容
    @RequestMapping("/getPendingReviewTicketList")
    public Result getPendingReviewTicketList(Map<String, Object> reqBody) {
        String selectFieldNames = "bss_order.*";
        String[] optionNames = {"order_name,deviceList,order_num,start_time,end_time,status,spe_user"};

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setOrderBy("end_time desc , start_time desc")
                .setWhere("status = 3")
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();

    }

    //查询 失效工作票内容
    @RequestMapping("/getInvalidTicketList")
    public Result getInvalidTicketList(Map<String, Object> reqBody) {
        String selectFieldNames = "bss_order.*";
        String[] optionNames = {"order_name,deviceList,order_num,start_time,end_time,status,spe_user"};

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setOrderBy("end_time desc , start_time desc")
                .setWhere("status = 7")
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();

    }

    //查询 历史工作票内容
    @RequestMapping("/getHistoryTicketList")
    public Result getHistoryTicketList(Map<String, Object> reqBody) {
        String selectFieldNames = "bss_order.*";
        String[] optionNames = {"order_name,deviceList,order_num,start_time,end_time,status,spe_user"};
        String currentTime = "CURRENT_TIMESTAMP";

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setOrderBy("end_time desc , start_time desc")
                .setWhere("end_time < " + currentTime)
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();

    }

    //查询 进行中工作票内容
    @RequestMapping("/getProgressTicketList")
    public Result getProgressTicketList(Map<String, Object> reqBody) {
        String selectFieldNames = "bss_order.*";
        String[] optionNames = {"order_name,deviceList,order_num,start_time,end_time,status,spe_user"};
        String currentTime = "CURRENT_TIMESTAMP";

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setAcceptOptions(optionNames)
                .setOrderBy("end_time desc , start_time desc")
                .setWhere("end_time > " + currentTime + " AND start_time < " + currentTime)
                .setFields(selectFieldNames);
        return sqlUtil.selectForTotalRowsResult();

    }

    //撤回 工作票
    @RequestMapping("/WithdrawTicket")
    public Result WithdrawTicket(Map<String, Object> reqBody) {
        int result = new SqlUtil(reqBody)
                .setTable("bss_order")
                .setFields("status")
                .setWhere("order_id = ?", reqBody.get("order_id"))
                .update();
        if (result == 1) {
            return new Result(200, "工作票已撤回", "success");
        }
        return new Result(200, "撤回失败", "error");
    }

    // 草稿票删除功能
    @RequestMapping("/doDelete")
    public Result doDelete(Map<String, Object> reqBody) {
        String str = reqBody.get("order_id").toString();
        String[] ids = str.split(",");
        int rSet = 0;
        for (int i = 0; i < ids.length; i++) {
            Integer id = Integer.parseInt(ids[i]);
            rSet = simpleJDBC.update("delete from audit_bss_order where order_id=?", id);
        }
        for (int i = 0; i < ids.length; i++) {
            Integer id = Integer.parseInt(ids[i]);
            rSet = simpleJDBC.update("delete from bss_order where order_id=?", id);
        }
        if (rSet != 0) {
            return new Result("success", 200, "删除成功");
        }
        return new Result("error", 000, "删除失败");
    }

}
