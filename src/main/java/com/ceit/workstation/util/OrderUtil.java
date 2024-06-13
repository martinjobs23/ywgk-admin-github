package com.ceit.workstation.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

public class OrderUtil {

    private String order_id;
    private Integer workstation_id;
    private String workstation_name;
    private LocalDateTime operator_start_time;
    private LocalDateTime operator_end_time;
    private String location;
    private String operator_id;
    private String operator_name;

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public Integer getWorkstation_id() {
        return workstation_id;
    }

    public void setWorkstation_id(Integer workstation_id) {
        this.workstation_id = workstation_id;
    }

    public String getWorkstation_name() {
        return workstation_name;
    }

    public void setWorkstation_name(String workstation_name) {
        this.workstation_name = workstation_name;
    }



//    public void setOperator_num(Integer operator_num) {
//        this.operator_num = operator_num;
//    }

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOperator_id() {
        return operator_id;
    }

    public void setOperator_id(String operator_id) {
        this.operator_id = operator_id;
    }

    public String getOperator_name() {
        return operator_name;
    }

    public void setOperator_name(String operator_name) {
        this.operator_name = operator_name;
    }

    @Override
    public String toString() {
        return "OrderUtil{" +
                "order_id='" + order_id + '\'' +
                ", workstation_id=" + workstation_id +
                ", workstation_name='" + workstation_name + '\'' +
                ", operator_start_time=" + operator_start_time +
                ", operator_end_time=" + operator_end_time +
                ", location='" + location + '\'' +
                ", operator_id='" + operator_id + '\'' +
                ", operator_name='" + operator_name + '\'' +
                '}';
    }
}
