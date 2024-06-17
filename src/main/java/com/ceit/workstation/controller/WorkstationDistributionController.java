package com.ceit.workstation.controller;

import com.ceit.ioc.annotations.Autowired;
import com.ceit.ioc.annotations.Controller;
import com.ceit.ioc.annotations.RequestMapping;
import com.ceit.jdbc.SimpleJDBC;
import com.ceit.response.Result;
import com.ceit.utils.SqlUtil;
import com.ceit.workstation.service.WorkstationDistributionService;
import com.ceit.workstation.service.WorkstationManegementService;
import com.ceit.workstation.util.MessageUtil;

import java.util.List;
import java.util.Map;

@Controller("/workstationDistribution")
public class WorkstationDistributionController {

    @Autowired
    //private WorkstationManegementService workstationManegementService;
    private WorkstationDistributionService workstationDistributionService;

    @Autowired
    private SimpleJDBC simpleJDBC;

    //查询已注册设备
    @RequestMapping(value = "/getCheckList")
    public Result getCheckList(Map<String, Object> reqBody){
        return workstationDistributionService.getCheckList(reqBody);
    }

    @RequestMapping(value = "/checkWorkstationStatus")
    public Result checkWorkstationStatus(Map<String, Object> reqBody){
        return workstationDistributionService.checkWorkstationStatus(reqBody);
    }

    @RequestMapping(value = "/orderDistribute")
    public Result orderDistribute(MessageUtil messageUtil){
        return workstationDistributionService.orderDistribute(messageUtil);
    }

    @RequestMapping(value = "/workstationPwdEdit")
    public Result orderRedistribute(Map<String, Object> reqBody){
        return workstationDistributionService.workstationPwdEdit(reqBody);
    }
    @RequestMapping(value = "/workstationRedistribute")
    public Result workstationRedistribute(Map<String, Object> reqBody){

        return workstationDistributionService.workstationRedistribute(reqBody);
    }
    @RequestMapping("/getFreeWorkstationList")
    public Result getFreeWorkstationList(Map<String, Object> reqBody) {
        return workstationDistributionService.getFreeWorkstationList(reqBody);
    }
    @RequestMapping("/delete")
    public Result delete(Map<String, Object> reqBody) {
        return workstationDistributionService.delete(reqBody);
    }
}
