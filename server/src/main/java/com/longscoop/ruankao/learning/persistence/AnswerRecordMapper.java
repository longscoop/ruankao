package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.UUID;

@Mapper
public interface AnswerRecordMapper extends BaseMapper<AnswerRecordEntity> {

    @Update("""
            update answer_record
            set mastery_applied = true
            where id = #{answerId}
              and mastery_applied = false
            """)
    int claimMastery(@Param("answerId") UUID answerId);
}
