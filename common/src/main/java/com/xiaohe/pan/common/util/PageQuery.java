package com.xiaohe.pan.common.util;


public class PageQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 20;


    public Integer getPageNum() {
        return pageNum - 1;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}