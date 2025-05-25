package com.xiaohe.pan.server.web.enums;

public enum BoundMenuDirection {
    DOUBLE(0, "双向同步"),
    UP(1, "上传(本地->云端)"),
    DOWN(2, "下载(云端->本地)")
    ;
    private Integer code;

    private String desc;
    BoundMenuDirection(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public static BoundMenuDirection getByCode(Integer code) {
        BoundMenuDirection[] values = BoundMenuDirection.values();
        for (BoundMenuDirection value : values) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
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
}
