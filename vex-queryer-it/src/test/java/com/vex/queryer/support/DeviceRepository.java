package com.vex.queryer.support;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 测试用仓储 — 仅用于集成测试写入夹具数据。
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {
}