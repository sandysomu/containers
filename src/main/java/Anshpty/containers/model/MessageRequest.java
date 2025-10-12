package Anshpty.containers.model;

import jakarta.validation.constraints.NotBlank;


public class MessageRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    private String correlationId;

    public MessageRequest() {
    }

    public MessageRequest(String message, String correlationId) {
        this.message = message;
        this.correlationId = correlationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
