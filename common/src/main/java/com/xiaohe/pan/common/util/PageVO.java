package com.xiaohe.pan.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(chain = true)
public class PageVO<T> {
    private Integer total;
    private Integer pageNum;
    private Integer pageSize;
    private List<T> records;

    public PageVO<T> setRecords(List<T> records) {
        this.records = records;
        return this;
    }

    public PageVO<T> setPageNum(Integer pageNum) {
        this.pageNum = pageNum + 1;
        return this;
    }

    public PageVO<T> setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PageVO<T> setTotal(Integer total) {
        this.total = total;
        return this;
    }
}
