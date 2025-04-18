-- 创建数据库
CREATE DATABASE IF NOT EXISTS sync_pan
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 使用新建的数据库
USE sync_pan;
create table bound_menu
(
    id               bigint auto_increment
        primary key,
    device_id        bigint               not null comment '设备ID',
    direction        int        default 1 not null comment '同步方向, 1-上传，2-下载',
    local_path       varchar(255)         not null comment '本地目录路径',
    remote_menu_id   bigint               not null comment '云端目录id',
    remote_menu_path varchar(1024)        not null comment '云端目录路径',
    status           int        default 1 not null comment '同步状态, 0-死亡, 1-健康, 2-暂停',
    last_synced_at   datetime             null comment '最后同步时间',
    create_time      timestamp            null,
    update_time      timestamp            null,
    deleted          tinyint(1) default 0 null
)
    comment '绑定文件夹表' charset = utf8mb4;

create table device
(
    id             bigint auto_increment
        primary key,
    user_id        bigint                               not null comment '用户ID',
    device_name    varchar(50)                          not null comment '设备名称',
    device_key     varchar(64)                          not null comment '设备唯一标识',
    secret_id      bigint                               not null comment '同步密钥',
    last_heartbeat datetime                             null comment '最后心跳时间',
    status         tinyint(1) default 0                 not null comment '状态（0-死亡，1-健康，2-暂停，3-注册）',
    create_time    timestamp  default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    timestamp  default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        int        default 0                 not null,
    constraint device_key
        unique (device_key)
)
    comment '客户端设备表' charset = utf8mb4;

create index idx_last_heartbeat
    on device (last_heartbeat);

create index idx_user_id
    on device (user_id);

create table file
(
    id           bigint auto_increment
        primary key,
    file_name    varchar(255)         not null,
    file_size    bigint               null comment '文件大小，单位为字节',
    file_type    varchar(50)          null,
    menu_id      bigint               null comment '该文件所属的目录，顶级文件的menu_id为空',
    owner        bigint               not null comment '文件所属人',
    real_path    varchar(500)         null comment '文件的真实路径',
    display_path varchar(1024)        not null comment '展示在网盘中的路径',
    storage_type int                  null comment '存储方式，1-服务器，2-ALI_OSS，3-MinIO',
    create_time  timestamp            null,
    update_time  timestamp            null,
    deleted      tinyint(1) default 0 null,
    identifier   varchar(255)         not null comment '文件的唯一标识'
)
    charset = utf8mb4;

create table menu
(
    id           bigint auto_increment
        primary key,
    menu_name    varchar(255)         not null,
    menu_level   int                  null,
    parent_id    bigint               null,
    owner        bigint               null,
    display_path varchar(1024)        not null comment '网盘中对外展示的路径',
    bound        tinyint(1) default 0 not null comment '是否已经具有绑定目录',
    create_time  timestamp            null,
    update_time  timestamp            null,
    deleted      tinyint(1) default 0 null
)
    charset = utf8mb4;

create table secret
(
    id          bigint auto_increment
        primary key,
    user_id     bigint               not null comment '密钥所属用户',
    `key`       varchar(64)          not null comment '密钥名称',
    value       varchar(255)         not null comment '密钥加密后的内容',
    create_time timestamp            null,
    update_time timestamp            null,
    deleted     tinyint(1) default 0 null
);

create table user
(
    id          bigint auto_increment
        primary key,
    username    varchar(255)         not null,
    password    varchar(255)         not null,
    salt        varchar(50)          not null,
    question    varchar(255)         null,
    answer      varchar(255)         null,
    create_time timestamp            null,
    update_time timestamp            null,
    deleted     tinyint(1) default 0 null,
    constraint username
        unique (username)
)
    charset = utf8mb4;

insert into user(`username`, `password`, `salt`) values ('root', '123', 'asdfsafd');