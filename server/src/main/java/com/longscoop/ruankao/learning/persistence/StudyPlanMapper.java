package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlanEntity> {

    @Insert("""
            insert into study_plan(
                user_id, exam_id, plan_date, target_minutes,
                estimated_minutes, actual_minutes, completion_rate, status
            )
            values(
                #{userId}, #{examId}, #{planDate}, #{targetMinutes},
                #{estimatedMinutes}, 0, 0, 'ACTIVE'
            )
            on conflict (user_id, exam_id, plan_date) do nothing
            """)
    int insertIfAbsent(
            @Param("userId") long userId,
            @Param("examId") long examId,
            @Param("planDate") LocalDate planDate,
            @Param("targetMinutes") int targetMinutes,
            @Param("estimatedMinutes") int estimatedMinutes);
}
