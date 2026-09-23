package com.longscoop.ruankao.auth.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface AuthSessionMapper extends BaseMapper<AuthSessionEntity> {

    @Select("""
            select user_id
            from auth_session
            where token_hash = #{tokenHash}
              and expires_at > #{now}
            """)
    Long selectActiveUserId(
            @Param("tokenHash") String tokenHash,
            @Param("now") OffsetDateTime now);

    @Update("""
            update auth_session
            set last_used_at = #{now}
            where token_hash = #{tokenHash}
              and expires_at > #{now}
            """)
    int touch(@Param("tokenHash") String tokenHash, @Param("now") OffsetDateTime now);

    @Delete("delete from auth_session where token_hash = #{tokenHash}")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);
}
