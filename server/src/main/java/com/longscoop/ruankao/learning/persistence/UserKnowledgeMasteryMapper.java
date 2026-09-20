package com.longscoop.ruankao.learning.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserKnowledgeMasteryMapper extends BaseMapper<UserKnowledgeMasteryEntity> {

    @Insert("""
            insert into user_knowledge_mastery(
                user_id,
                knowledge_id,
                mastery_score,
                evidence_count,
                correct_streak,
                wrong_streak,
                last_effective_study_at,
                updated_at
            )
            values(
                #{userId},
                #{knowledgeId},
                greatest(0::numeric, least(100::numeric, cast(#{scoreDelta} as numeric))),
                1,
                case when #{correct} then 1 else 0 end,
                case when #{correct} then 0 else 1 end,
                now(),
                now()
            )
            on conflict (user_id, knowledge_id)
            do update set
                mastery_score = greatest(
                    0::numeric,
                    least(
                        100::numeric,
                        user_knowledge_mastery.mastery_score + cast(#{scoreDelta} as numeric)
                    )
                ),
                evidence_count = user_knowledge_mastery.evidence_count + 1,
                correct_streak = case
                    when #{correct} then user_knowledge_mastery.correct_streak + 1
                    else 0
                end,
                wrong_streak = case
                    when #{correct} then 0
                    else user_knowledge_mastery.wrong_streak + 1
                end,
                last_effective_study_at = now(),
                updated_at = now()
            """)
    int applyDelta(
            @Param("userId") long userId,
            @Param("knowledgeId") long knowledgeId,
            @Param("scoreDelta") double scoreDelta,
            @Param("correct") boolean correct);
}
