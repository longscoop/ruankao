package com.longscoop.ruankao.content.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import java.util.UUID;

@TableName("content_import_page")
public class ContentImportPageEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private UUID batchId; private Integer pageNumber; private String textContent;
    private String imageObjectKey; private OffsetDateTime createdAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public UUID getBatchId(){return batchId;} public void setBatchId(UUID v){batchId=v;}
    public Integer getPageNumber(){return pageNumber;} public void setPageNumber(Integer v){pageNumber=v;}
    public String getTextContent(){return textContent;} public void setTextContent(String v){textContent=v;}
    public String getImageObjectKey(){return imageObjectKey;} public void setImageObjectKey(String v){imageObjectKey=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
}
