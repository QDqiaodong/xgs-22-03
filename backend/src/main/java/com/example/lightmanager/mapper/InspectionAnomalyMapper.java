package com.example.lightmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lightmanager.dto.AnomalyPageRequestDTO;
import com.example.lightmanager.entity.InspectionAnomaly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InspectionAnomalyMapper extends BaseMapper<InspectionAnomaly> {

    /** 异常清单：关联灯组当前分区，标出分区是否已变化 */
    IPage<InspectionAnomaly> selectAnomalyPage(IPage<InspectionAnomaly> page,
                                               @Param("query") AnomalyPageRequestDTO query);

    void batchInsert(@Param("anomalies") List<InspectionAnomaly> anomalies);

    // ===== 受控状态流转（CAS：带 id + version + 期望状态，版本自增） =====

    /** 退回：待复核 -> 已退回 */
    int casReturn(@Param("id") Long id,
                  @Param("version") Integer version,
                  @Param("operator") String operator,
                  @Param("reason") String reason,
                  @Param("time") LocalDateTime time);

    /** 补充：已退回 -> 待复核 */
    int casSupplement(@Param("id") Long id,
                      @Param("version") Integer version,
                      @Param("supplementDescription") String supplementDescription,
                      @Param("operator") String operator,
                      @Param("time") LocalDateTime time);

    /** 确认：待复核 -> 已确认 */
    int casConfirm(@Param("id") Long id,
                   @Param("version") Integer version,
                   @Param("operator") String operator,
                   @Param("reviewOpinion") String reviewOpinion,
                   @Param("deadline") LocalDateTime deadline,
                   @Param("time") LocalDateTime time);

    /** 登记处理结果：已确认 -> 处理中 */
    int casResolve(@Param("id") Long id,
                   @Param("version") Integer version,
                   @Param("operator") String operator,
                   @Param("handlingResult") String handlingResult,
                   @Param("time") LocalDateTime time);

    /** 关闭：处理中 -> 已关闭（关闭后终态，不可回退） */
    int casClose(@Param("id") Long id,
                 @Param("version") Integer version,
                 @Param("operator") String operator,
                 @Param("time") LocalDateTime time);
}
