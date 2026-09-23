package com.longscoop.ruankao.course.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoMapper extends BaseMapper<VideoEntity> {

    @Select("""
            select v.*
            from video v
            join video_knowledge_relation r on r.video_id = v.id
            where r.knowledge_id = #{knowledgeId}
            order by v.sort_order, v.id
            """)
    List<VideoEntity> selectByKnowledgePointId(long knowledgeId);
}
