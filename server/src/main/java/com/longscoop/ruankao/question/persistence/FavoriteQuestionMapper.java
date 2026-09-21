package com.longscoop.ruankao.question.persistence;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FavoriteQuestionMapper {

    @Insert("""
            insert into favorite_question(user_id, question_id, created_at)
            values(#{userId}, #{questionId}, now())
            on conflict (user_id, question_id) do nothing
            """)
    int add(@Param("userId") long userId, @Param("questionId") long questionId);

    @Delete("""
            delete from favorite_question
            where user_id = #{userId}
              and question_id = #{questionId}
            """)
    int remove(@Param("userId") long userId, @Param("questionId") long questionId);

    @Select("""
            select user_id, question_id, created_at
            from favorite_question
            where user_id = #{userId}
            order by created_at desc, question_id desc
            """)
    List<FavoriteQuestionEntity> list(@Param("userId") long userId);
}
