package com.vex.queryer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页参数 — page 从 0 起始;对齐 ProTable current-1。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueriesPage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码;从 0 起始;ProTable current-1
     */
    private int page;
    /**
     * 每页大小;默认 20
     */
    private int size;

}
