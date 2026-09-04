package com.freelancesuite.config;

import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.security.CustomUserDetailsService;
import com.freelancesuite.security.JwtTokenProvider;
import com.freelancesuite.security.UserPrincipal;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ProjectRepository projectRepository;

    public WebSocketConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins,
                           JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService,
                           ProjectRepository projectRepository) {
        this.allowedOrigins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.projectRepository = projectRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new AccessDeniedException("WebSocket authentication is required");
                    }
                    String token = authorization.substring(7);
                    if (!tokenProvider.validateToken(token)) {
                        throw new AccessDeniedException("Invalid WebSocket token");
                    }
                    UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserById(tokenProvider.getUserIdFromJWT(token));
                    accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)
                            || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                        throw new AccessDeniedException("WebSocket authentication is required");
                    }
                    boolean isStaff = principal.getAuthorities().stream()
                            .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER") || authority.getAuthority().equals("ROLE_MEMBER"));
                    String destination = accessor.getDestination();
                    java.util.regex.Matcher matcher = destination == null ? null
                            : java.util.regex.Pattern.compile("^/topic/project/(\\d+)(?:/comments)?$").matcher(destination);
                    if (!isStaff || matcher == null || !matcher.matches()
                            || projectRepository.findByIdAndAgencyId(Long.valueOf(matcher.group(1)), principal.getAgencyId()).isEmpty()) {
                        throw new AccessDeniedException("You cannot subscribe to this project");
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}
