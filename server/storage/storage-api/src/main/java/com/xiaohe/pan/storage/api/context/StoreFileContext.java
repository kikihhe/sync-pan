package com.xiaohe.pan.storage.api.context;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 文件存储引擎存储物理文件的上下文实体
 */
@Data
@Accessors(chain = true)
public class StoreFileContext implements Serializable {

    private static final long serialVersionUID = -514678100134294180L;

    /**
     * 上传的文件名称
     */
    private String filename;

    /**
     * 文件的总大小
     */
    private Long totalSize;

    /**
     * 文件的输入流信息
     */
    private InputStream inputStream;

    /**
     * 文件上传后的物理路径
     */
    private String realPath;

}
