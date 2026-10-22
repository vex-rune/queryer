package com.vex.queryer.support;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 测试用设备角色枚举 — 带 {@code @JsonValue} 对外值,验证按外部值归一。
 */
public enum DeviceRole {
    /** 网关 */
    GATEWAY("gw"),
    /** 传感器 */
    SENSOR("sn"),
    /** 摄像头 */
    CAMERA("cm");

    private final String code;

    DeviceRole(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
