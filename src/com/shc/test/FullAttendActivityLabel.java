package com.shc.test;

/**
 * @Author: haichao.shao
 * @Description: aa
 * @Date: 2020-05-21 16:53
 */
public enum FullAttendActivityLabel {
    //0（不能申请），1（可以申请），2（已经申请过退款），3（已经申请过优惠券）
    none(0, "不能申请"),
    fullAttend(1, "全勤奖励"),
    refunded(2, "已退款"),
    acquiredCoupons(3, "已经领取优惠券");
    private Integer code;
    private String label;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    FullAttendActivityLabel(Integer code, String label) {
        this.code = code;
        this.label = label;
    }
    public static String getLabel(Integer value) {
        FullAttendActivityLabel[] attendActivityLabels = values();
        for (FullAttendActivityLabel attendActivityLabel : attendActivityLabels) {
            if (attendActivityLabel.code.equals(value)) {
                return attendActivityLabel.label;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.printf(FullAttendActivityLabel.getLabel(1));
    }

}
