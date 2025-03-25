package com.xiaohe.pan.common.enums;

public enum SyncCommandEnum {
    SYNC_NOW(1, "sync_now"),
    WAIT(2, "wait")
    ;

    SyncCommandEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private Integer code;
    private String desc;

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
}
