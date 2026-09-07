package site.yesaido.user_server.domain.inquiry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCategoryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.controller.docs.AdminInquiryControllerDocs;
import site.yesaido.user_server.domain.inquiry.entity.InquiryStatus;
import site.yesaido.user_server.domain.inquiry.service.InquiryService;
import site.yesaido.user_server.global.common.ApiResponse;
import site.yesaido.user_server.global.common.PageRequestValidator;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController implements AdminInquiryControllerDocs {
    private final InquiryService inquiryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InquirySummaryResponse>>> getAllInquirySummary(
            @RequestHeader("X-User-Id") Long adminUserId,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Pageable pageable = PageRequestValidator.of(page, size);
        Page<InquirySummaryResponse> responses = inquiryService.getAllInquiries(adminUserId, status, pageable);
        ApiResponse<Page<InquirySummaryResponse>> apiResponse = ApiResponse.ok("전체 문의 목록입니다.", responses);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    @GetMapping("/{inquiry-id}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getInquiryDetail(@RequestHeader("X-User-Id") Long adminUserId,
                                                                               @PathVariable("inquiry-id") Long inquiryId) {
        InquiryDetailResponse response = inquiryService.getInquiryDetailForAdmin(adminUserId, inquiryId);
        ApiResponse<InquiryDetailResponse> apiResponse = ApiResponse.ok("문의 상세입니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    @PutMapping(value = "/messages/{answer-id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> answerMessage(@RequestHeader("X-User-Id") Long adminUserId,
                                                                            @PathVariable("answer-id") Long answerId,
                                                                            @Valid @RequestPart("request") InquiryMessageRequest inquiryMessageRequest,
                                                                            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        InquiryDetailResponse response = inquiryService.answerMessage(adminUserId, answerId, inquiryMessageRequest, files);
        ApiResponse<InquiryDetailResponse> apiResponse = ApiResponse.ok("답변이 등록되었습니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<InquiryCategoryResponse>> createCategory(@RequestHeader("X-User-Id") Long adminId, @Valid @RequestBody InquiryCategoryCreateRequest request){
        InquiryCategoryResponse response = inquiryService.createCategory(adminId, request);
        ApiResponse<InquiryCategoryResponse> apiResponse = ApiResponse.created("문의 카테고리가 등록되었습니다.", response);

        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
