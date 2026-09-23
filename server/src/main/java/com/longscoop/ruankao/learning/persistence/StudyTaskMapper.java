package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface StudyTaskMapper extends BaseMapper<StudyTaskEntity> {

    @Select("""
            select count(*)
            from study_task t
            join study_plan p on p.id = t.plan_id
            where p.user_id = #{userId}
              and p.plan_date >= #{startDate}
              and p.plan_date <= #{endDate}
              and t.status = 'COMPLETED'
            """)
    long countCompletedBetween(
            @Param("userId") long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
