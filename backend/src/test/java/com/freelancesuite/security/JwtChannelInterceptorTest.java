package com.freelancesuite.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtChannelInterceptorTest {

    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(null, null, null);
    private final MessageChannel channel = new MessageChannel() {
        @Override
        public boolean send(Message<?> message) {
            return true;
        }

        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    };

    @Test
    void clientsCannotSendMessagesToBrokerTopicsOrApplicationDestinations() {
        assertThrows(AccessDeniedException.class,
                () -> interceptSend("/topic/project/42/comments"));
        assertThrows(AccessDeniedException.class,
                () -> interceptSend("/app/comments"));
    }

    private void interceptSend(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, channel);
    }
}
