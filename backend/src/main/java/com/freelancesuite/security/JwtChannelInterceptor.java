package com.freelancesuite.security;

import com.freelancesuite.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Without this, /ws/** is permitAll at the HTTP layer (required for the SockJS
 * handshake) but nothing validated *who* was on the other end of a STOMP
 * CONNECT — any client on an allowed origin could open a session and
 * subscribe to any project's comment stream. This closes that gap:
 *   - CONNECT requires a valid Bearer JWT in the STOMP "Authorization" header.
 *   - SUBSCRIBE to /topic/project/{id}/comments is checked against the
 *     authenticated user's agency, mirroring TaskCommentService's HTTP-side check.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern PROJECT_COMMENTS_TOPIC =
            Pattern.compile("^/topic/project/(\\d+)/comments$");

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ProjectRepository projectRepository;

    @Autowired
    public JwtChannelInterceptor(JwtTokenProvider tokenProvider,
                                  CustomUserDetailsService userDetailsService,
                                  ProjectRepository projectRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.projectRepository = projectRepository;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Authentication authentication = authenticate(accessor);
            accessor.setUser(authentication);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor);
        if (token == null || !tokenProvider.validateToken(token)) {
            throw new BadCredentialsException("A valid session is required to open a live connection");
        }
        Long userId = tokenProvider.getUserIdFromJWT(token);
        UserDetails userDetails = userDetailsService.loadUserById(userId);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher matcher = PROJECT_COMMENTS_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            // Not a project-comments topic — nothing extra to check here.
            return;
        }

        Object principal = accessor.getUser();
        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)
                || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new AccessDeniedException("You must be authenticated to subscribe to this topic");
        }

        Long projectId = Long.valueOf(matcher.group(1));
        boolean allowed = projectRepository.findByIdAndAgencyId(projectId, userPrincipal.getAgencyId()).isPresent();
        if (!allowed) {
            throw new AccessDeniedException("You cannot subscribe to comments for this project");
        }
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String header = authHeaders.get(0);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
