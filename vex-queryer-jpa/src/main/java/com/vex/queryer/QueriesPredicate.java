package com.vex.queryer;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 查询条件组合 — 支持 AND/OR 嵌套;同一层级 and/or 可并存,按各自语义合并。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueriesPredicate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * AND 条件列表;null/空 = 不参与 AND 合并
     */
    @Valid
    private List<QueriesCriteria> and;
    /**
     * OR 条件列表;null/空 = 不参与 OR 合并
     */
    @Valid
    private List<QueriesCriteria> or;

}
