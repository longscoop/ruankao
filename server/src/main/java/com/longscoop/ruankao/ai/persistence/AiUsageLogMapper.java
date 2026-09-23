package com.longscoop.ruankao.ai.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;

@Mapper
public interface AiUsageLogMapper extends BaseMapper<AiUsageLogEntity> {

    @Select("""
            select count(*)
            from ai_usage_log
            where user_id = #{userId}
              and created_at >= #{since}
              and success = true
            """)
    long countSuccessfulSince(
            @Param("userId") long userId,
            @Param("since") OffsetDateTime since);
}
