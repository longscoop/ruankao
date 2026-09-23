package com.longscoop.ruankao.assessment.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.UUID;

@Mapper
public interface AssessmentItemMapper extends BaseMapper<AssessmentItemEntity> {

    @Update("""
            update assessment_item
            set answer_record_id = #{answerRecordId}
            where session_id = #{sessionId}
              and question_id = #{questionId}
              and answer_record_id is null
            """)
    int attachAnswer(
            @Param("sessionId") UUID sessionId,
            @Param("questionId") long questionId,
            @Param("answerRecordId") UUID answerRecordId);
}
