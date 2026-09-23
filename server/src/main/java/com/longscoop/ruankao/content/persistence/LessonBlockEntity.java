package com.longscoop.ruankao.content.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.content.model.LessonBlockType;

import java.time.OffsetDateTime;

@TableName("lesson_block")
public class LessonBlockEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonId;
    private LessonBlockType blockType;
    private String textContent;
    private String imageObjectKey;
    private Integer sourcePage;
    private Integer sortOrder;
    private OffsetDateTime createdAt;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getLessonId(){return lessonId;} public void setLessonId(Long v){lessonId=v;}
    public LessonBlockType getBlockType(){return blockType;} public void setBlockType(LessonBlockType v){blockType=v;}
    public String getTextContent(){return textContent;} public void setTextContent(String v){textContent=v;}
    public String getImageObjectKey(){return imageObjectKey;} public void setImageObjectKey(String v){imageObjectKey=v;}
    public Integer getSourcePage(){return sourcePage;} public void setSourcePage(Integer v){sourcePage=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
}
