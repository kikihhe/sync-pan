package com.xiaohe.pan.server.web.constants;

public class RedisConstants {
    public static class MenuRedisConstants {
        /**
         * 记录所有目录的size，hash 结构
         * KEY: SIZE_HASH
         * FIELD: menu_id
         * VALUE: size
         */
        public static final String MENU_SIZE_KEY = "menu:SIZE_HASH";

        /**
         * 记录目录的所有父目录
         * key: menu:PARENT:{menu_id}
         * value: parent id list
         */
        public static final String MENU_PARENT_KEY = "menu:PARENT:";


        /**
         * 记录目录的所有子目录
         * key: menu:CHILDREN:{menu_id}
         * value: children id list
         */
        public static final String MENU_CHILDREN_KEY = "menu:CHILDREN:";
    }

    public static class FileRedisConstants {

    }
}
