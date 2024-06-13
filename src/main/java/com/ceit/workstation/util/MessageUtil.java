package com.ceit.workstation.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

public class MessageUtil {
    private String transId; //工作流水号
    private String order_id;
    ArrayList<OrderUtil> operatorList = new ArrayList<OrderUtil>();
    private Integer operator_num = operatorList.size();
    private String location;
    private LocalDateTime operator_start_time;
    private LocalDateTime operator_end_time;

    public ArrayList<OrderUtil> getOperatorList() {
        return operatorList;
    }

    public void setOperatorList(ArrayList<OrderUtil> operatorList) {
        this.operatorList = operatorList;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }
    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }
    public Integer getOperator_num() {
        return operator_num;
    }
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getOperator_start_time() {
        return operator_start_time;
    }

    public void setOperator_start_time(LocalDateTime operator_start_time) {
        this.operator_start_time = operator_start_time;
    }

    public LocalDateTime getOperator_end_time() {
        return operator_end_time;
    }

    public void setOperator_end_time(LocalDateTime operator_end_time) {
        this.operator_end_time = operator_end_time;
    }
}
