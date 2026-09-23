package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
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

    @Insert("""
            insert into answer_record(
                id, user_id, question_id, session_id, answer, correct,
                duration_seconds, confidence, source, idempotency_key,
                mastery_applied, answered_at
            )
            values(
                #{record.id}, #{record.userId}, #{record.questionId}, #{record.sessionId},
                #{record.answer}, #{record.correct}, #{record.durationSeconds},
                #{record.confidence}, #{record.source}, #{record.idempotencyKey},
                false, now()
            )
            on conflict (user_id, idempotency_key)
            where idempotency_key is not null
            do nothing
            """)
    int insertIdempotent(@Param("record") AnswerRecordEntity record);

    @Select("""
            select *
            from answer_record
            where user_id = #{userId}
              and idempotency_key = #{idempotencyKey}
            """)
    AnswerRecordEntity selectByIdempotencyKey(
            @Param("userId") long userId,
            @Param("idempotencyKey") String idempotencyKey);

    @Select("""
            select count(*)
            from answer_record
            where user_id = #{userId}
              and answered_at >= #{since}
              and answered_at < #{until}
            """)
    long countAnsweredBetween(
            @Param("userId") long userId,
            @Param("since") OffsetDateTime since,
            @Param("until") OffsetDateTime until);

    @Select("""
            select count(*)
            from answer_record
            where user_id = #{userId}
              and correct = true
              and answered_at >= #{since}
              and answered_at < #{until}
            """)
    long countCorrectBetween(
            @Param("userId") long userId,
            @Param("since") OffsetDateTime since,
            @Param("until") OffsetDateTime until);

    @Select("""
            select coalesce(sum(duration_seconds), 0)
            from answer_record
            where user_id = #{userId}
              and answered_at >= #{since}
              and answered_at < #{until}
            """)
    long sumDurationSecondsBetween(
            @Param("userId") long userId,
            @Param("since") OffsetDateTime since,
            @Param("until") OffsetDateTime until);

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
