package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AiChatRequest {
    @NotBlank(message = "消息不能为空")
    @Size(max = 1000, message = "单条消息不能超过1000字")
    private String message;
    private List<Message> history;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Message> getHistory() { return history; }
    public void setHistory(List<Message> history) { this.history = history; }

    public static class Message {
        private String role;
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
