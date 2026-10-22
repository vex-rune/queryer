package com.vex.queryer;

/**
 * 查询限制器 — 限定一次查询的规模边界,防止构造出巨型/深嵌套的查询拖垮存储。
 *
 * <p>承载三类上限:
 * <ul>
 *     <li>条件总数上限(maxConditions)</li>
 *     <li>排序字段数上限(maxOrders)</li>
 *     <li>谓词嵌套深度上限(maxDepth;根谓词计为第 1 层,每嵌套一层 +1)</li>
 * </ul>
 * 执行器构造期注入;不注入时使用 {@link #DEFAULT}(默认 200 / 10 / 3)。
 *
 * @param maxConditions 单次查询最大条件数
 * @param maxOrders     单次查询最大排序字段数
 * @param maxDepth      谓词嵌套最大深度
 */
public record QueryLimits(int maxConditions, int maxOrders, int maxDepth) {

    /**
     * 默认限制 — 条件 200 / 排序 10 / 嵌套深度 3
     */
    public static final QueryLimits DEFAULT = new QueryLimits(200, 10, 3);

    /**
     * 紧凑构造 — 各上限必须为正数,否则视为配置错误。
     */
    public QueryLimits {
        if (maxConditions < 1 || maxOrders < 1 || maxDepth < 1) {
            throw new IllegalArgumentException("QueryLimits 各上限必须为正数: "
                    + "maxConditions=" + maxConditions + ", maxOrders=" + maxOrders + ", maxDepth=" + maxDepth);
        }
    }
}
