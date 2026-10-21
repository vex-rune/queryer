package com.vex.queryer.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 集成测试实体 — 覆盖执行器需要验证的各列类型。
 *
 * <p>刻意包含 timestamptz 时间列与枚举列:这两类正是 JSON 反序列化后类型丢失、
 * 在真实 PG 上会触发 {@code operator does not exist} 的场景。
 */
@Entity
@Table(name = "it_device")
public class DeviceEntity {

    /** 主键;雪花风格字符串 id */
    @Id
    @Column(name = "id", length = 64)
    private String id;

    /** 设备编码;唯一,用于 eq/like 验证 */
    @Column(name = "code", length = 64)
    private String code;

    /** 设备名称;用于 like / exp 正则验证 */
    @Column(name = "name", length = 128)
    private String name;

    /** 设备状态枚举;用于枚举值归一验证 */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_status", length = 32)
    private DeviceStatus deviceStatus;

    /** 设备角色枚举;验证 @JsonValue 外部值归一 */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_role", length = 32)
    private DeviceRole deviceRole;

    /** 排序权重;用于数值比较与 in 验证 */
    @Column(name = "weight")
    private Integer weight;

    /** 是否启用;用于布尔归一验证 */
    @Column(name = "enabled")
    private Boolean enabled;

    /** 创建时间(timestamptz);核心验证目标 — 字符串直接比较会报 SQL 语法错 */
    @Column(name = "created_at")
    private Instant createdAt;

    /** 备注;可空,用于 isNull / isNotNull 验证 */
    @Column(name = "remark", length = 256)
    private String remark;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public DeviceRole getDeviceRole() {
        return deviceRole;
    }

    public void setDeviceRole(DeviceRole deviceRole) {
        this.deviceRole = deviceRole;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}