package com.shc.test;

/**
 * @Author: haichao.shao
 * @Description: a
 * @Date: 2020-05-19 18:43
 */
public enum FullAttendRewardType {
    refund("退款"),coupon("优惠券");

    private final String desc;

    public String getDesc() {
        return desc;
    }

    FullAttendRewardType(String desc) {
        this.desc = desc;
    }
}
