package com.vex.queryer.support;

/**
 * 测试用设备状态枚举 — 无 {@code @JsonValue},验证按 name 匹配。
 */
public enum DeviceStatus {
    /** 草稿 */
    DRAFT,
    /** 在线 */
    ONLINE,
    /** 离线 */
    OFFLINE
}
