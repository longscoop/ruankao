package com.longscoop.ruankao.question.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestionEntity> {

    @Insert("""
            insert into wrong_question(
                user_id,
                question_id,
                status,
                wrong_count,
                consecutive_correct,
                first_wrong_at,
                last_wrong_at,
                mastered_at,
                updated_at
            )
            values(
                #{userId},
                #{questionId},
                'ACTIVE',
                1,
                0,
                now(),
                now(),
                null,
                now()
            )
            on conflict (user_id, question_id)
            do update set
                status = 'ACTIVE',
                wrong_count = wrong_question.wrong_count + 1,
                consecutive_correct = 0,
                last_wrong_at = now(),
                mastered_at = null,
                updated_at = now()
            """)
    int recordWrong(
            @Param("userId") long userId,
            @Param("questionId") long questionId);

    @Update("""
            update wrong_question
            set
                consecutive_correct = consecutive_correct + 1,
                status = case
                    when status = 'MASTERED' then 'MASTERED'
                    when consecutive_correct + 1 >= 2 and #{effectiveMastery} >= 70
                        then 'MASTERED'
                    else 'ACTIVE'
                end,
                mastered_at = case
                    when status = 'MASTERED' then mastered_at
                    when consecutive_correct + 1 >= 2 and #{effectiveMastery} >= 70
                        then now()
                    else null
                end,
                updated_at = now()
            where user_id = #{userId}
              and question_id = #{questionId}
            """)
    int recordCorrectReview(
            @Param("userId") long userId,
            @Param("questionId") long questionId,
            @Param("effectiveMastery") double effectiveMastery);
}
