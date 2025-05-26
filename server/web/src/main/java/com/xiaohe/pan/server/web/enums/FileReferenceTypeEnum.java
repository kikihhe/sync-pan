package com.xiaohe.pan.server.web.enums;

public enum FileReferenceTypeEnum {
    FILE(1, "文件"),
    CHUNK(2, "分片");

    private final int code;
    private final String desc;

    FileReferenceTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static FileReferenceTypeEnum getByCode(int code) {
        for (FileReferenceTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}