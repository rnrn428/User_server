package site.yesaido.user_server.domain.inquiry.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryAccessResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.global.common.ApiResponse;

import java.util.List;

/**
 * {@code InquiryController}의 OpenAPI 문서 정의.
 */
@Tag(name = "문의", description = "1:1 문의 카테고리 조회 · 등록 · 내 문의 목록/상세 · 추가 질문 · 접근 권한 확인")
public interface InquiryControllerDocs {

    @Operation(summary = "문의 카테고리 목록", description = "문의 등록 시 선택할 수 있는 카테고리 목록을 반환합니다.")
    ResponseEntity<ApiResponse<List<InquiryCategoryResponse>>> getCategories();

    @Operation(summary = "문의 등록",
            description = "`request`(JSON) + `files`(선택) multipart 로 새 문의를 등록합니다.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록됨"))
    ResponseEntity<ApiResponse<InquiryDetailResponse>> createInquiry(
            Long userId, InquiryCreateRequest request, List<MultipartFile> files);

    @Operation(summary = "내 문의 목록", description = "요청자 본인이 등록한 문의 목록을 페이지로 반환합니다.")
    ResponseEntity<ApiResponse<Page<InquirySummaryResponse>>> getInquiries(
            Long userId,
            @Parameter(description = "0-based 페이지 번호") Integer page,
            @Parameter(description = "페이지 크기") Integer size);

    @Operation(summary = "내 문의 상세", description = "요청자 본인 문의의 상세(메시지/첨부 포함)를 반환합니다.")
    ResponseEntity<ApiResponse<InquiryDetailResponse>> getMyInquiryDetail(
            Long userId, @Parameter(description = "문의 ID") Long inquiryId);

    @Operation(summary = "추가 질문 등록", description = "기존 문의 스레드에 추가 질문(메시지/첨부)을 남깁니다.")
    ResponseEntity<ApiResponse<InquiryDetailResponse>> addFollowUp(
            Long userId,
            @Parameter(description = "문의 ID") Long inquiryId,
            InquiryMessageRequest request,
            List<MultipartFile> files);

    @Operation(summary = "문의 접근 권한 확인", description = "요청자가 해당 문의를 볼 수 있는지 검증합니다. (알림 서비스 연동용)")
    ResponseEntity<ApiResponse<InquiryAccessResponse>> checkInquiryAccess(
            Long userId, @Parameter(description = "문의 ID") Long inquiryId);
}
