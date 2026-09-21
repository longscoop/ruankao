package com.longscoop.ruankao.content.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.common.persistence.JsonbStringTypeHandler;
import com.longscoop.ruankao.content.model.ImportItemStatus;
import com.longscoop.ruankao.content.model.ImportItemType;
import java.time.OffsetDateTime;
import java.util.UUID;

@TableName(value = "content_import_item", autoResultMap = true)
public class ContentImportItemEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private UUID batchId; private ImportItemType itemType; private String itemKey;
    private Integer sourcePageStart; private Integer sourcePageEnd; private String title;
    @TableField(typeHandler = JsonbStringTypeHandler.class) private String contentJson;
    private ImportItemStatus status; private Long targetId; private Integer sortOrder;
    private OffsetDateTime createdAt; private OffsetDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public UUID getBatchId(){return batchId;} public void setBatchId(UUID v){batchId=v;}
    public ImportItemType getItemType(){return itemType;} public void setItemType(ImportItemType v){itemType=v;}
    public String getItemKey(){return itemKey;} public void setItemKey(String v){itemKey=v;}
    public Integer getSourcePageStart(){return sourcePageStart;} public void setSourcePageStart(Integer v){sourcePageStart=v;}
    public Integer getSourcePageEnd(){return sourcePageEnd;} public void setSourcePageEnd(Integer v){sourcePageEnd=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContentJson(){return contentJson;} public void setContentJson(String v){contentJson=v;}
    public ImportItemStatus getStatus(){return status;} public void setStatus(ImportItemStatus v){status=v;}
    public Long getTargetId(){return targetId;} public void setTargetId(Long v){targetId=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
}
