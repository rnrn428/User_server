package site.yesaido.user_server.domain.inquiry.dto.response;

import site.yesaido.user_server.domain.inquiry.entity.Inquiry;
import site.yesaido.user_server.domain.inquiry.entity.InquiryAnswer;
import site.yesaido.user_server.domain.inquiry.entity.InquiryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public record InquiryDetailResponse(
        Long id,
        Long userId,
        String userNickname,
        Long categoryId,
        String categoryName,
        String title,
        InquiryStatus status,
        LocalDateTime createdAt,
        Long cultivationId,
        String cultivationName,
        List<InquiryMessageResponse> messages
) {
    public static InquiryDetailResponse of(Inquiry inquiry, List<InquiryAnswer> answer, String cultivationName,
                                           String userNickname, Function<String, String> photoUrlResolver) {
        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getUserId(),
                userNickname,
                inquiry.getCategory().getId(),
                inquiry.getCategory().getCategoryName(),
                inquiry.getTitle(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getCultivationId(),
                cultivationName,
                answer.stream().map(a -> InquiryMessageResponse.from(a, photoUrlResolver)).toList()
        );
    }
}