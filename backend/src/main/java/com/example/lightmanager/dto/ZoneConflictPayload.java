package com.example.lightmanager.dto;

import com.example.lightmanager.entity.GarageZone;

import java.util.List;

/**
 * 分区版本冲突（409）载荷：携带该分区最新内容与按最新父子关系重建的整棵树，
 * 后确认的管理员据此重新展示后再决定是否重试。
 */
public class ZoneConflictPayload {

    /** 冲突分区的最新内容（最新上级/层级/版本） */
    private GarageZone latestZone;

    /** 最新分区树 */
    private List<GarageZone> latestTree;

    public ZoneConflictPayload() {}

    public ZoneConflictPayload(GarageZone latestZone, List<GarageZone> latestTree) {
        this.latestZone = latestZone;
        this.latestTree = latestTree;
    }

    public GarageZone getLatestZone() { return latestZone; }
    public void setLatestZone(GarageZone latestZone) { this.latestZone = latestZone; }
    public List<GarageZone> getLatestTree() { return latestTree; }
    public void setLatestTree(List<GarageZone> latestTree) { this.latestTree = latestTree; }
}
