package com.ceit.admin.controller;

import com.ceit.admin.service.IndexService;
import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.utils.SqlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *主页信息
 */
@Controller("/index")
public class IndexController {

    @Autowired
    IndexService indexService;

    /**
     * 获取网关信息
     * @param reqBody
     * @return
     */
    @RequestMapping("/getGateWayInfo")
    public String getGateWayInfo(Map<String, Object> reqBody) {
        String jsonData = indexService.getGateWayInfo(reqBody);
        return jsonData;
    }


    /**
     * 获取cpu利用率数据
     * @param reqBody
     * @return
     */
    @RequestMapping("/getCpuList")
    public Map getCpuList(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        List cpuList = sqlUtil.setTable("login.sys_cpu")
                .setOrderBy("check_time desc limit 7")
                .selectForMapList();
        List<Double> usage_rate = new ArrayList();
        List<String> check_time = new ArrayList();
        //倒序
        for (int i = cpuList.size() - 1; i >= 0 ; i--) {
            Map<String, Object> map = (Map<String, Object>) cpuList.get(i);
            usage_rate.add((Double) map.get("usage_rate"));
            check_time.add((String) map.get("check_time"));
        }
        Map<String, List> map = new HashMap<>();
        map.put("usage_rate", usage_rate);
        map.put("check_time",check_time);
        return map;
    }

    /**
     * 获取内存利用率数据
     * @param reqBody
     * @return
     */
    @RequestMapping("/getMemoryList")
    public Map getMemoryList(Map<String, Object> reqBody) {

        SqlUtil sqlUtil = new SqlUtil(reqBody);
        List memoryList = sqlUtil.setTable("login.sys_memory")
                .setOrderBy("check_time desc limit 7")
                .selectForMapList();
        List<Double> usage_rate = new ArrayList();
        List<String> check_time = new ArrayList();
        for (int i = memoryList.size() - 1; i >= 0 ; i--) {
            Map<String, Object> map = (Map<String, Object>) memoryList.get(i);
            usage_rate.add((Double) map.get("usage_rate"));
            check_time.add((String) map.get("check_time"));
        }
        Map<String, List> map = new HashMap<>();
        map.put("usage_rate", usage_rate);
        map.put("check_time",check_time);
        return map;
    }
}
