package site.yesaido.user_server.domain.inquiry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryAccessResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.service.InquiryService;
import site.yesaido.user_server.global.common.ApiResponse;
import site.yesaido.user_server.global.common.PageRequestValidator;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiries")
public class InquiryController {
    private final InquiryService inquiryService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<InquiryCategoryResponse>>> getCategories() {
        List<InquiryCategoryResponse> responses = inquiryService.getCategories();
        ApiResponse<List<InquiryCategoryResponse>> apiResponse = ApiResponse.ok("문의 카테고리 목록입니다.", responses);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> createInquiry(@RequestHeader("X-User-Id") Long userId,
                                                                            @Valid @RequestPart("request") InquiryCreateRequest request,
                                                                            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        InquiryDetailResponse response = inquiryService.createInquiry(userId, request, files);
        ApiResponse<InquiryDetailResponse> apiResponse = ApiResponse.created("문의가 등록되었습니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InquirySummaryResponse>>> getInquiries(@RequestHeader("X-User-Id") Long userId,
                                                                                  @RequestParam(defaultValue = "0") Integer page,
                                                                                  @RequestParam(defaultValue = "20") Integer size) {
        Pageable pageable = PageRequestValidator.of(page, size);
        Page<InquirySummaryResponse> responses = inquiryService.getMyInquiries(userId, pageable);
        ApiResponse<Page<InquirySummaryResponse>> apiResponse = ApiResponse.ok("내 문의 목록입니다.", responses);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @GetMapping("/{inquiry-id}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getMyInquiryDetail(@RequestHeader("X-User-Id") Long userId,
                                                                            @PathVariable("inquiry-id") Long inquiryId) {
        InquiryDetailResponse response = inquiryService.getMyInquiryDetail(userId, inquiryId);
        ApiResponse<InquiryDetailResponse> apiResponse = ApiResponse.ok("문의 상세입니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PostMapping(value = "/{inquiry-id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> addFollowUp(@RequestHeader("X-User-Id") Long userId,
                                                                          @PathVariable("inquiry-id") Long inquiryId,
                                                                          @Valid @RequestPart("request") InquiryMessageRequest request,
                                                                          @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        InquiryDetailResponse response = inquiryService.addFollowUp(userId, inquiryId, request, files);
        ApiResponse<InquiryDetailResponse> apiResponse = ApiResponse.ok("추가 질문이 등록되었습니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @GetMapping("/{inquiry-id}/access")
    public ResponseEntity<ApiResponse<InquiryAccessResponse>> checkInquiryAccess(@RequestHeader("X-User-Id") Long userId,
                                                                                 @PathVariable("inquiry-id") Long inquiryId){
        boolean allowed = inquiryService.canAccessInquiry(userId, inquiryId);
        ApiResponse<InquiryAccessResponse> apiResponse = ApiResponse.ok("문의 접근 권한 검증 결과입니다.", new InquiryAccessResponse(allowed));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
