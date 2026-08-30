package site.yesaido.user_server.domain.inquiry.dto.response;

import site.yesaido.user_server.domain.inquiry.entity.InquiryAnswer;
import site.yesaido.user_server.domain.inquiry.entity.InquiryPhoto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public record InquiryMessageResponse(
        Long id,
        Long preId,
        String content,
        String answerContent,
        LocalDateTime createdAt,
        List<String> photoUrls
) {
    public static InquiryMessageResponse from(InquiryAnswer answer, Function<String, String> photoUrlResolver) {
        return new InquiryMessageResponse(
                answer.getId(),
                answer.getPre() != null ? answer.getPre().getId() : null,
                answer.getContent(),
                answer.getAnswerContent(),
                answer.getCreatedAt(),
                answer.getInquiryPhotos().stream()
                        .map(InquiryPhoto::getObjectKey)
                        .map(photoUrlResolver)
                        .toList()
        );
    }
}