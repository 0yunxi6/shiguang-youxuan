package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("customer_ticket")
public class CustomerTicket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long assigneeId;
    private String ticketNo;
    private String source;
    private String type;
    private String title;
    private String content;
    private String orderNo;
    private Integer priority;
    private Integer status;
    private String resolution;
    private Integer satisfaction;
    private String satisfactionRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private LocalDateTime closedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public Integer getSatisfaction() { return satisfaction; }
    public void setSatisfaction(Integer satisfaction) { this.satisfaction = satisfaction; }
    public String getSatisfactionRemark() { return satisfactionRemark; }
    public void setSatisfactionRemark(String satisfactionRemark) { this.satisfactionRemark = satisfactionRemark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public LocalDateTime getClosedTime() { return closedTime; }
    public void setClosedTime(LocalDateTime closedTime) { this.closedTime = closedTime; }
}
