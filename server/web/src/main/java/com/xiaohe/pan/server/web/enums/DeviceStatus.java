package com.xiaohe.pan.server.web.enums;

public enum DeviceStatus {
    DIE(0, "死亡"),
    HEALTH(1, "健康"),
    STOP_SYNC(2, "停止同步"),
    REGISTER(3, "注册"),
    DELETED(4, "删除"),
    ;
    private Integer code;
    private String desc;

    DeviceStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static DeviceStatus getByCode(Integer code) {
        for (DeviceStatus deviceStatus : DeviceStatus.values()) {
            if (deviceStatus.getCode().equals(code)) {
                return deviceStatus;
            }
        }
        return null;
    }
}
