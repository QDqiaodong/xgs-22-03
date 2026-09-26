package com.example.lightmanager.service;

import com.example.lightmanager.entity.GarageZone;

import java.util.List;

public interface GarageZoneService {

    List<GarageZone> getZoneTree();

    GarageZone getZoneById(Long id);

    GarageZone createZone(GarageZone zone);

    /**
     * 编辑分区（含换父级）：
     * - 换父级基于最新层级关系做环检测（自挂 / 挂到任意后代均拒绝，反馈冲突分区）；
     * - 携带读取时的 version 做 CAS，过期提交得到 409 版本冲突与最新树，不覆盖先确认的结果；
     * - 换父级在同一事务内整体平移后代 level，子树结构与灯组归属不变，失败整体回滚。
     */
    GarageZone updateZone(GarageZone zone);

    /**
     * 删除分区：仅允许删除“无任何子分区、分区及后代无灯组、无未核销划转流水”的空叶子；
     * 其余情况 412 并返回逐条阻断原因（列出具体冲突分区）。
     */
    void deleteZone(Long id);

    List<GarageZone> getAllZones();
}
