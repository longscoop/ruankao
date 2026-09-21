package com.longscoop.ruankao.course.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserVideoProgressMapper {

    @Select("select * from user_video_progress where user_id = #{userId} and video_id = #{videoId}")
    UserVideoProgressEntity select(long userId, long videoId);

    @Insert("""
            insert into user_video_progress(
                user_id, video_id, progress_seconds, completion_rate, completed, mastery_applied, last_watch_at, updated_at
            ) values (
                #{userId}, #{videoId}, #{progressSeconds}, #{completionRate}, #{completed}, false, now(), now()
            ) on conflict (user_id, video_id) do update set
                progress_seconds = greatest(user_video_progress.progress_seconds, excluded.progress_seconds),
                completion_rate = greatest(user_video_progress.completion_rate, excluded.completion_rate),
                completed = user_video_progress.completed or excluded.completed,
                last_watch_at = now(),
                updated_at = now()
            """)
    int upsert(
            @Param("userId") long userId,
            @Param("videoId") long videoId,
            @Param("progressSeconds") int progressSeconds,
            @Param("completionRate") double completionRate,
            @Param("completed") boolean completed);

    @Update("""
            update user_video_progress
            set mastery_applied = true, updated_at = now()
            where user_id = #{userId}
              and video_id = #{videoId}
              and completed = true
              and mastery_applied = false
            """)
    int claimMastery(@Param("userId") long userId, @Param("videoId") long videoId);

    @Select("""
            select coalesce(sum(progress_seconds), 0)
            from user_video_progress
            where user_id = #{userId}
              and last_watch_at >= #{since}
              and last_watch_at < #{until}
            """)
    long sumProgressSecondsBetween(
            @Param("userId") long userId,
            @Param("since") java.time.OffsetDateTime since,
            @Param("until") java.time.OffsetDateTime until);
}
