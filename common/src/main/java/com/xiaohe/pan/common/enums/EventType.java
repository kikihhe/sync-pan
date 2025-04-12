package com.xiaohe.pan.common.enums;

import lombok.Getter;


@Getter
public enum EventType {
        FILE_CREATE(1, "创建文件"),
        FILE_MODIFY(2, "修改文件"),
        FILE_DELETE(3, "删除文件"),
        DIRECTORY_CREATE(4, "创建目录"),
        DIRECTORY_MODIFY(5, "修改目录"),
        DIRECTORY_DELETE(6, "删除目录")
        ;
        private int code;
        private String desc;

        EventType(int code, String desc) {
                this.code = code;
                this.desc = desc;
        }

}