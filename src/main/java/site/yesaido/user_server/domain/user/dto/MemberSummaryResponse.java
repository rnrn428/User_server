package site.yesaido.user_server.domain.user.dto;

import site.yesaido.user_server.domain.user.entity.User;

import java.time.LocalDateTime;

public record MemberSummaryResponse(
        Long userId,
        String nickname,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime deletedAt
) {
    public static MemberSummaryResponse from(User user) {
        return new MemberSummaryResponse(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt(),
                user.getDeletedAt()
        );
    }
}
