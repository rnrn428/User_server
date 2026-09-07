package site.yesaido.user_server.domain.inquiry.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCategoryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.entity.InquiryStatus;
import site.yesaido.user_server.global.common.ApiResponse;

import java.util.List;

/**
 * {@code AdminInquiryController}의 OpenAPI 문서 정의.
 */
@Tag(name = "관리자 - 문의", description = "전체 문의 목록/상세 조회 · 답변 등록 · 카테고리 생성 (관리자 전용)")
public interface AdminInquiryControllerDocs {

    @Operation(summary = "전체 문의 목록", description = "상태(status)별 전체 문의 목록을 페이지로 반환합니다.")
    ResponseEntity<ApiResponse<Page<InquirySummaryResponse>>> getAllInquirySummary(
            Long adminUserId,
            @Parameter(description = "문의 상태 필터") InquiryStatus status,
            @Parameter(description = "0-based 페이지 번호") Integer page,
            @Parameter(description = "페이지 크기") Integer size);

    @Operation(summary = "문의 상세 (관리자)", description = "관리자 권한으로 특정 문의의 상세를 조회합니다.")
    ResponseEntity<ApiResponse<InquiryDetailResponse>> getInquiryDetail(
            Long adminUserId, @Parameter(description = "문의 ID") Long inquiryId);

    @Operation(summary = "문의 답변 등록",
            description = "특정 답변 슬롯(answer-id)에 답변 내용/첨부를 등록합니다. `request`(JSON) + `files`(선택) multipart.")
    ResponseEntity<ApiResponse<InquiryDetailResponse>> answerMessage(
            Long adminUserId,
            @Parameter(description = "답변 ID") Long answerId,
            InquiryMessageRequest inquiryMessageRequest,
            List<MultipartFile> files);

    @Operation(summary = "문의 카테고리 생성",
            description = "새 문의 카테고리를 등록합니다.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록됨"))
    ResponseEntity<ApiResponse<InquiryCategoryResponse>> createCategory(
            Long adminId, InquiryCategoryCreateRequest request);
}
