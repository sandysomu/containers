package Anshpty.containers.controller;
import Anshpty.containers.model.MessageRequest;
import Anshpty.containers.model.MessageResponse;
import Anshpty.containers.service.MessagePublisher;
import Anshpty.containers.service.MessageSubscriber;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private MessageSubscriber messageSubscriber;

    @PostMapping("/publish")
    public ResponseEntity<MessageResponse> publishMessage(@Valid @RequestBody MessageRequest request) {
        try {
            messagePublisher.publishMessage(request.getMessage(), request.getCorrelationId());
            return ResponseEntity.ok(MessageResponse.success("Message published successfully"));
        } catch (Exception e) {
            log.error("Error publishing message", e);
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.error("Failed to publish message: " + e.getMessage()));
        }
    }

    @GetMapping("/received")
    public ResponseEntity<MessageResponse> getReceivedMessages() {
        List<String> messages = messageSubscriber.getReceivedMessages();
        Map<String, Object> data = new HashMap<>();
        data.put("messages", messages);
        data.put("count", messages.size());

        return ResponseEntity.ok(MessageResponse.success("Retrieved received messages", data));
    }

    @GetMapping("/count")
    public ResponseEntity<MessageResponse> getMessageCount() {
        int count = messageSubscriber.getMessageCount();
        return ResponseEntity.ok(MessageResponse.success("Message count retrieved",
                Map.of("count", count)));
    }

    @DeleteMapping("/received")
    public ResponseEntity<MessageResponse> clearReceivedMessages() {
        messageSubscriber.clearReceivedMessages();
        return ResponseEntity.ok(MessageResponse.success("Received messages cleared"));
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(MessageResponse.success("Solace messaging service is healthy"));
    }

}
