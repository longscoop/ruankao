package com.longscoop.ruankao.content.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;

@TableName("lesson_knowledge")
public class LessonKnowledgeEntity {
    private Long lessonId;
    private Long knowledgeId;
    private OffsetDateTime createdAt;
    public Long getLessonId(){return lessonId;} public void setLessonId(Long v){lessonId=v;}
    public Long getKnowledgeId(){return knowledgeId;} public void setKnowledgeId(Long v){knowledgeId=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
}
