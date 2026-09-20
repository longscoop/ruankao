package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
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

    @Select("""
            select count(*)
            from answer_record ar
            join question_knowledge qk on qk.question_id = ar.question_id
            where ar.user_id = #{userId}
              and qk.knowledge_id = #{knowledgeId}
              and ar.correct = false
              and ar.answered_at >= #{since}
            """)
    Integer countRecentWrongByKnowledge(
            @Param("userId") long userId,
            @Param("knowledgeId") long knowledgeId,
            @Param("since") OffsetDateTime since);
}
