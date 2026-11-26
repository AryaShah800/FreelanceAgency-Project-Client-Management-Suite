package com.freelancesuite.security;

import com.freelancesuite.entity.Client;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.enums.Role;
import com.freelancesuite.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtChannelInterceptorTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(null, null, projectRepository);
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

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OWNER", "ADMIN", "MEMBER"})
    void internalUsersCanSubscribeToBothFeedsInTheirAgency(Role role) {
        when(projectRepository.findByIdAndAgencyId(42L, 1L)).thenReturn(Optional.of(new Project()));

        assertDoesNotThrow(() -> interceptSubscribe("/topic/project/42", principal(role, 1L)));
        assertDoesNotThrow(() -> interceptSubscribe("/topic/project/42/comments", principal(role, 1L)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"client@example.com", "someone-else@example.com"})
    void clientsCannotSubscribeToCommentsEvenWhenTheirEmailMatches(String clientEmail) {
        Client client = new Client();
        client.setEmail(clientEmail);
        Project project = new Project();
        project.setClient(client);
        when(projectRepository.findByIdAndAgencyId(42L, 1L)).thenReturn(Optional.of(project));

        assertThrows(AccessDeniedException.class,
                () -> interceptSubscribe("/topic/project/42/comments", principal(Role.CLIENT, 1L)));
    }

    @Test
    void clientsCannotSubscribeToTaskEvents() {
        assertThrows(AccessDeniedException.class,
                () -> interceptSubscribe("/topic/project/42", principal(Role.CLIENT, 1L)));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OWNER", "ADMIN", "MEMBER"})
    void otherAgencyUsersCannotSubscribeToEitherFeed(Role role) {
        when(projectRepository.findByIdAndAgencyId(42L, 2L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> interceptSubscribe("/topic/project/42", principal(role, 2L)));
        assertThrows(AccessDeniedException.class,
                () -> interceptSubscribe("/topic/project/42/comments", principal(role, 2L)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "/topic/tasks", "/topic/unknown", "/queue/updates", "/user/queue/updates",
            "/app/comments", "/topic/project/42/private", "/topic/project/42/comments/extra",
            "/topic/project/42/", "/topic/project/*", "/topic/project/not-an-id",
            "/topic/project/999999999999999999999999"})
    void unlistedOrInvalidDestinationsAreDeniedEvenForOwners(String destination) {
        assertThrows(AccessDeniedException.class,
                () -> interceptSubscribe(destination, principal(Role.OWNER, 1L)));
    }

    @Test
    void unauthenticatedSubscriptionsAreDenied() {
        assertThrows(AccessDeniedException.class,
                () -> interceptSubscribe("/topic/project/42/comments", null));
    }

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

    private UserPrincipal principal(Role role, Long agencyId) {
        return new UserPrincipal(7L, "Test user", "client@example.com", "", agencyId, role,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    private void interceptSubscribe(String destination, UserPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        if (destination != null) accessor.setDestination(destination);
        if (principal != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        }
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, channel);
    }
}
