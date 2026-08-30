package site.yesaido.user_server.domain.inquiry.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCategoryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.entity.InquiryStatus;
import site.yesaido.user_server.domain.inquiry.service.InquiryService;
import site.yesaido.user_server.global.common.ApiResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdminInquiryControllerTest {

    @Mock
    private InquiryService inquiryService;

    @InjectMocks
    private AdminInquiryController adminInquiryController;

    @Test
    @DisplayName("GET /api/admin/inquiries - 전체 문의 목록 조회 성공")
    void getAllInquirySummary_success() {
        Page<InquirySummaryResponse> page = new PageImpl<>(List.of(mock(InquirySummaryResponse.class)));
        given(inquiryService.getAllInquiries(eq(99L), eq(InquiryStatus.PENDING), any(Pageable.class))).willReturn(page);

        ResponseEntity<ApiResponse<Page<InquirySummaryResponse>>> response =
                adminInquiryController.getAllInquirySummary(99L, InquiryStatus.PENDING, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/admin/inquiries/{inquiry-id} - 관리자 문의 상세 조회 성공")
    void getInquiryDetail_success() {
        InquiryDetailResponse detailResponse = mock(InquiryDetailResponse.class);
        given(inquiryService.getInquiryDetailForAdmin(99L, 10L)).willReturn(detailResponse);

        ResponseEntity<ApiResponse<InquiryDetailResponse>> response =
                adminInquiryController.getInquiryDetail(99L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(detailResponse);
    }

    @Test
    @DisplayName("PUT /api/admin/inquiries/messages/{answer-id} - 관리자 답변 작성 성공")
    void answerMessage_success() {
        InquiryMessageRequest request = new InquiryMessageRequest("답변 내용");
        List<MultipartFile> files = List.of(mock(MultipartFile.class));
        InquiryDetailResponse detailResponse = mock(InquiryDetailResponse.class);
        given(inquiryService.answerMessage(99L, 50L, request, files)).willReturn(detailResponse);

        ResponseEntity<ApiResponse<InquiryDetailResponse>> response =
                adminInquiryController.answerMessage(99L, 50L, request, files);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(detailResponse);
    }

    @Test
    @DisplayName("POST /api/admin/inquiries/categories - 관리자 카테고리 생성 성공")
    void createCategory_success() {
        InquiryCategoryCreateRequest request = new InquiryCategoryCreateRequest("신규 카테고리");
        InquiryCategoryResponse categoryResponse = new InquiryCategoryResponse(10L, "신규 카테고리");
        given(inquiryService.createCategory(99L, request)).willReturn(categoryResponse);

        ResponseEntity<ApiResponse<InquiryCategoryResponse>> response =
                adminInquiryController.createCategory(99L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().categoryName()).isEqualTo("신규 카테고리");
    }
}
