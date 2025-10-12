package Anshpty.containers.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;
    private Object data;

    public static MessageResponse success(String message) {
        return MessageResponse.builder()
                .status("success")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static MessageResponse success(String message, Object data) {
        return MessageResponse.builder()
                .status("success")
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    public static MessageResponse error(String message) {
        return MessageResponse.builder()
                .status("error")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
