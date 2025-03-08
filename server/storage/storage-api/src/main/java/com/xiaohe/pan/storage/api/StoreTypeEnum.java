package com.xiaohe.pan.storage.api;

import java.util.Objects;

public enum StoreTypeEnum {
    LOCAL(1, "local"),
    ALI_OSS(2, "ali_oss"),
    MINIO(3, "minio")
    ;
    private Integer code;
    private String desc;

    StoreTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static StoreTypeEnum getByCode(Integer code) {
        if (Objects.isNull(code)) {
            return null;
        }
        for (StoreTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static Integer getCodeByDesc(String desc) {
        if (Objects.isNull(desc)) {
            return null;
        }
        for (StoreTypeEnum value : values()) {
            if (value.desc.equals(desc)) {
                return value.code;
            }
        }
        return null;
    }
}
