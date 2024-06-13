package com.ceit.index;

import com.ceit.bootstrap.ConfigLoader;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;

import java.util.Map;
import java.util.Objects;

// 主页
@Controller("/index")
public class IndexController {

    @Autowired
    SimpleJDBC simpleJDBC;

    //统计在线终端
    @RequestMapping("/getOnlineTerminalNums")
    public Result getOnlineTerminalNums(Map<String, Object> reqBody){
        String sql = "SELECT \"在线\" AS name,COUNT(*) AS value FROM dev_cert WHERE `online`=1 UNION SELECT \"离线\" AS name,COUNT(*) AS value FROM dev_cert WHERE `online`=0";
        String jsonData = ""; //simpleJDBC.selectForJsonArray(sql);
        sql = "SELECT COUNT(*) FROM dev_cert";
        int count = simpleJDBC.selectForOneInt(sql);
        String data = "{\"totalCount\":" + count +",\"jsonData\":" + jsonData + "}";
        return new Result("success",200,data);
    }

}