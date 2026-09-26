package com.example.lightmanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 报废送审 / 退回补充后再次送审入参。
 * 首次送审：报废理由、现场照片说明、生效日期必填。
 * 再次送审：补充内容必填，原始理由以历史首条为准（可一并回传，仅用于页面展示）。
 */
public class ArchiveSubmitDTO {

    /** 操作人（管理员） */
    @NotBlank(message = "操作人不能为空")
    private String operator;

    /** 打开详情时的归档版本号，用于乐观并发控制 */
    @NotNull(message = "版本号不能为空")
    private Integer version;

    /** 报废理由（首次送审必填，再次送审不覆盖原始理由） */
    private String scrapReason;

    /** 补充内容（退回后再次送审必填） */
    private String supplementNote;

    /** 现场照片说明（首次送审必填） */
    private String photoNote;

    /** 报废生效日期（首次送审必填） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getScrapReason() { return scrapReason; }
    public void setScrapReason(String scrapReason) { this.scrapReason = scrapReason; }
    public String getSupplementNote() { return supplementNote; }
    public void setSupplementNote(String supplementNote) { this.supplementNote = supplementNote; }
    public String getPhotoNote() { return photoNote; }
    public void setPhotoNote(String photoNote) { this.photoNote = photoNote; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
}
