package com.ceit.log;


import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

@Controller("/systemLog")
public class QueryLogsController {
    @RequestMapping("/getsercuityAudit")
    public Result getsercuityAudit(Map<String, Object> reqBody)
    {


        int page = reqBody.get("page") != null ? Integer.parseInt(reqBody.get("page").toString()) : 1;
        int rows = reqBody.get("rows") != null ? Integer.parseInt(reqBody.get("rows").toString()) : 10;

//        reqBody.put("pageNow", page);
//        reqBody.put("pageSize", rows);

        SqlUtil sqlUtil = null;


            sqlUtil = new SqlUtil(reqBody)
                    .setTable("log")
                    .setFields("*")
                    .setAcceptOptions("EVENT_RECORD", "TYPE", "EVENT_NAME")
                    .setSearchFields("EVENT_RECORD", "TYPE", "EVENT_NAME");

        //查询数据总数，当前分页数据
        return sqlUtil.selectForTotalRowsResult();


    }
    @RequestMapping("/getaccountlogs")
    public Result logs(Map<String, Object> reqBody)
    {
        //数据库表，数据字段
        String tableName ="accountchange_log";

        String[] selectFieldNames = { "*" };

        String where = "";
        //允许用户从前端通过searchName和searchValue设置的搜索条件
        String[] searchOptionNames = {"userId",
                "ACCOUNT",
                "NAME",
                "detail",
                "ip",
        };
        //特殊处理 根据userId查询
        String userId = (String)reqBody.get("userId");
        if(userId!=null && userId.trim().length()>0)
        {
            if(where.length() > 0)
                where +=" and ";

            where += "userId = '" + userId +"'";
        }

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable(tableName)
                .setFields(selectFieldNames)
                .setAcceptOptions(searchOptionNames)
                .setSearchFields(searchOptionNames)
                .setWhere(where)
                .setOrderBy("createTime desc");

        Result result = sqlUtil.selectForTotalRowsResult();
        System.out.println("data:" + result.getData());
        //查询数据总数，当前分页数据
        return sqlUtil.selectForTotalRowsResult();
    }

    //运维操作日志
    @RequestMapping("/getUserOperationList")
    public String getUserOperationList(Map<String, Object> reqBody) {

        int page = reqBody.get("page") != null ? Integer.parseInt(reqBody.get("page").toString()) : 1;
        int rows = reqBody.get("rows") != null ? Integer.parseInt(reqBody.get("rows").toString()) : 10;
//        String searchName = (String) reqBody.get("searchName");
//        String searchValue = (String) reqBody.get("searchValue");
//
//        reqBody.put("pageNow", page);
//        reqBody.put("pageSize", rows);
////        reqBody.put("option", searchName);
////        reqBody.put("condition", searchValue);
//
//        if (searchName != null && searchName.equals("b.sequenceSegmentValue")) {
//            if (searchValue.contains("务") || searchValue.contains("业")) {
//                reqBody.put("searchValue", "1");
//            } else if (searchValue.contains("巡检") || searchValue.contains("巡视")) {
//                reqBody.put("searchValue", "2");
//            } else if (searchValue.contains("检修")) {
//                reqBody.put("searchValue", "3");
//            } else if (searchValue.contains("抢修")) {
//                reqBody.put("searchValue", "4");
//            } else if (searchValue.contains("项目")) {
//                reqBody.put("searchValue", "5");
//            } else if (searchValue.contains("白名单") || searchValue.contains("作业") || searchValue.contains("白")) {
//                reqBody.put("searchValue", "6");
//            } else if (searchValue.contains("系统")) {
//                reqBody.put("searchValue", "7");
//            } else if (searchValue.contains("部署")) {
//                reqBody.put("searchValue", "8");
//            } else {
//                reqBody.put("searchValue", searchValue);
//            }
//        }

        String where = "u.order_Id = b.order_Id";
        SqlUtil sqlUtil = null;

            sqlUtil = new SqlUtil(reqBody)
                    .setTable("user_login_log u,bss_order1 b ")
                    .setFields("u.*,b.order_name as order_name,b.sequenceSegmentValue")
                    .setWhere(where)
                    .setAcceptOptions("yunweiuser_account", "yunweiuser_name", "yunweiuser_ip", "order_name", "device_name", "u.device_ip", "device_pro", "device_user", "b.sequenceSegmentValue", "u.login_time", "u.logout_time")
                    .setSearchFields("yunweiuser_account", "yunweiuser_name", "yunweiuser_ip", "order_name", "device_name", "u.device_ip", "device_pro", "device_user", "b.sequenceSegmentValue", "u.login_time", "u.logout_time")
                    .setOrderBy("u.login_time desc");



        String jsonData = "[]";
        int total = sqlUtil.selectForTotalCount();

        if (total > 0) {
            List mapList = sqlUtil.selectForMapList();
            JSONArray jsonArray = new JSONArray(mapList);
            JSONArray resultArray = new JSONArray();
            //遍历每一个行数据
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String sequenceSegmentValue = null;
                try {
                    sequenceSegmentValue = jsonObject.getString("sequenceSegmentValue");
                } catch (Exception e) {
                    jsonObject.put("sequenceSegmentValue", "");

                }

                String login_time=null;
                try {
                    login_time = jsonObject.getString("login_time");
                    login_time=login_time.replace("T"," ");
                    jsonObject.put("login_time",login_time);
                } catch (Exception e) {
                    jsonObject.put("login_time", "");

                }

                String logout_time=null;
                try {
                    logout_time = jsonObject.getString("logout_time");
                    logout_time=logout_time.replace("T"," ");
                    jsonObject.put("logout_time",logout_time);
                } catch (Exception e) {
                    jsonObject.put("logout_time", "");

                }

                if (sequenceSegmentValue.equals("1"))
                    jsonObject.put("sequenceSegmentValue", "业务运维模式");
                else if (sequenceSegmentValue.equals("2"))
                    jsonObject.put("sequenceSegmentValue", "巡检/巡视工作模式");
                else if (sequenceSegmentValue.equals("3"))
                    jsonObject.put("sequenceSegmentValue", "检修工作模式");
                else if (sequenceSegmentValue.equals("4"))
                    jsonObject.put("sequenceSegmentValue", "抢修工作模式");
                else if (sequenceSegmentValue.equals("5"))
                    jsonObject.put("sequenceSegmentValue", "项目实施工作模式");
                else if (sequenceSegmentValue.equals("6"))
                    jsonObject.put("sequenceSegmentValue", "白名单作业模式");
                else if (sequenceSegmentValue.equals("7"))
                    jsonObject.put("sequenceSegmentValue", "系统运维模式");
                else if (sequenceSegmentValue.equals("8"))
                    jsonObject.put("sequenceSegmentValue", "部署实施模式");


                resultArray.put(jsonObject);
            }
            jsonData = resultArray.toString();
        }
        jsonData = "{\"total\":" + total + ",\"rows\":" + jsonData + "}";

        return jsonData;
    }

    @RequestMapping("/getStrategyList")
    public Result getStrategyList(Map<String,Object> reqBody){
        String acceptOptions ="cmd,is_confirmed,user_name,NAME,accountMarkers,num,order_name,sequenceSegmentValue,target_ip," +
                "peopleProperty,isKey,user_ip,device_name,runningStatus";
        Object startTime = reqBody.get("startTime");
        String start=String.valueOf(startTime);
        Object endTime = reqBody.get("endTime");
        String end = String.valueOf(endTime);
        String where="is_confirmed='是'";
        if(start!="null")
        {
            where+=" and cmd_time >= \'"+start+"\'";

        }
        if(end!="null")
        {
            where+=" and cmd_time <= \'"+end+"\'";
        }


        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("all_cmd_log")
                .setFields("*")
                .setAcceptOptions(acceptOptions)
                .setSearchFields(acceptOptions)
                .setWhere(where)
                .setOrderBy("cmd_time desc") ;;

        return sqlUtil.selectForTotalRowsResult();

    }

    @RequestMapping("/getTerminalHardwareAlterList")
    public Result getAllLogList(Map<String, Object> reqBody)   {
//        Integer page = (Integer) reqBody.get("page");
//        Integer rows = (Integer) reqBody.get("rows");
//        String offset = String.valueOf(( page - 1 ) * rows );
//        String searchName = (String) reqBody.get("searchName");
//        String searchValue = (String) reqBody.get("searchValue");
//
//        //select
//        String select = "select *";
//
//        //from
//        String from = " from ( select id from all_cmd_log order by cmd_time desc limit " + offset + "," + rows +" ) acl1 " +
//                "left join all_cmd_log acl2 on acl1.id = acl2.id";
//
//        //where
//        String where = searchName == null || searchValue == null ?
//                "" :
//                " where " + searchName + " like \"%" + searchValue + "%\"";
//
//        SimpleJDBC simpleJDBC = SimpleJDBC.getInstance();
//        String jsonData = simpleJDBC.selectForJsonArray(select + from + where);
//        String total = simpleJDBC.selectForOneString("select count(*) from all_cmd_log");

        String acceptOptions ="cmd,is_confirmed,user_name,NAME,accountMarkers,num,order_name,sequenceSegmentValue,target_ip," +
                "peopleProperty,isKey,user_ip,device_name,runningStatus,organize_name,cmd_time,peopleProperty,group_name";

        SqlUtil sqlUtil = new SqlUtil(reqBody)
                .setTable("all_cmd_log")
                .setFields("*")
                .setAcceptOptions(acceptOptions)
                .setSearchFields(acceptOptions)
                .setOrderBy("cmd_time desc") ;

        //查询数据总数，当前分页数据
        return sqlUtil.selectForTotalRowsResult();
    }

}
