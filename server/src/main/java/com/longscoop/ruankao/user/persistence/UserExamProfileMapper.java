package com.longscoop.ruankao.user.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.longscoop.ruankao.user.model.FoundationLevel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface UserExamProfileMapper extends BaseMapper<UserExamProfileEntity> {

    @Insert("""
            insert into user_exam_profile(
                user_id,
                exam_id,
                exam_date,
                daily_target_minutes,
                foundation_level,
                assessment_completed,
                created_at,
                updated_at
            )
            values(
                #{userId},
                #{examId},
                #{examDate},
                #{dailyTargetMinutes},
                #{foundationLevel},
                false,
                now(),
                now()
            )
            on conflict (user_id)
            do update set
                exam_id = excluded.exam_id,
                exam_date = excluded.exam_date,
                daily_target_minutes = excluded.daily_target_minutes,
                foundation_level = excluded.foundation_level,
                assessment_completed = case
                    when user_exam_profile.exam_id = excluded.exam_id
                        then user_exam_profile.assessment_completed
                    else false
                end,
                updated_at = now()
            """)
    int upsert(
            @Param("userId") long userId,
            @Param("examId") long examId,
            @Param("examDate") LocalDate examDate,
            @Param("dailyTargetMinutes") int dailyTargetMinutes,
            @Param("foundationLevel") FoundationLevel foundationLevel);
}
