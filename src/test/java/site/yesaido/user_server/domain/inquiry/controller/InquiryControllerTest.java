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
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryAccessResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.service.InquiryService;
import site.yesaido.user_server.global.common.ApiResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InquiryControllerTest {

    @Mock
    private InquiryService inquiryService;

    @InjectMocks
    private InquiryController inquiryController;

    @Test
    @DisplayName("GET /api/inquiries/categories - 카테고리 목록 조회 성공")
    void getCategories_success() {
        InquiryCategoryResponse categoryResponse = new InquiryCategoryResponse(1L, "재배 문의");
        given(inquiryService.getCategories()).willReturn(List.of(categoryResponse));

        ResponseEntity<ApiResponse<List<InquiryCategoryResponse>>> response = inquiryController.getCategories();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().getFirst().categoryName()).isEqualTo("재배 문의");
    }

    @Test
    @DisplayName("POST /api/inquiries - 문의 등록 성공")
    void createInquiry_success() {
        InquiryCreateRequest request = new InquiryCreateRequest(1L, "제목", "내용", null);
        List<MultipartFile> files = List.of(mock(MultipartFile.class));
        InquiryDetailResponse detailResponse = mock(InquiryDetailResponse.class);

        given(inquiryService.createInquiry(1L, request, files)).willReturn(detailResponse);

        ResponseEntity<ApiResponse<InquiryDetailResponse>> response = inquiryController.createInquiry(1L, request, files);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(detailResponse);
    }

    @Test
    @DisplayName("GET /api/inquiries - 내 문의 목록 페이징 조회 성공")
    void getInquiries_success() {
        Page<InquirySummaryResponse> page = new PageImpl<>(List.of(mock(InquirySummaryResponse.class)));
        given(inquiryService.getMyInquiries(eq(1L), any(Pageable.class))).willReturn(page);

        ResponseEntity<ApiResponse<Page<InquirySummaryResponse>>> response = inquiryController.getInquiries(1L, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/inquiries/{inquiry-id} - 내 문의 상세 조회 성공")
    void getMyInquiryDetail_success() {
        InquiryDetailResponse detailResponse = mock(InquiryDetailResponse.class);
        given(inquiryService.getMyInquiryDetail(1L, 10L)).willReturn(detailResponse);

        ResponseEntity<ApiResponse<InquiryDetailResponse>> response = inquiryController.getMyInquiryDetail(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(detailResponse);
    }

    @Test
    @DisplayName("POST /api/inquiries/{inquiry-id}/messages - 추가 질문 등록 성공")
    void addFollowUp_success() {
        InquiryMessageRequest request = new InquiryMessageRequest("추가 내용");
        InquiryDetailResponse detailResponse = mock(InquiryDetailResponse.class);
        given(inquiryService.addFollowUp(1L, 10L, request)).willReturn(detailResponse);

        ResponseEntity<ApiResponse<InquiryDetailResponse>> response = inquiryController.addFollowUp(1L, 10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(detailResponse);
    }

    @Test
    @DisplayName("GET /api/v1/inquiries/{inquiry-id}/access - 문의 접근 권한 검증 결과 반환")
    void checkInquiryAccess_success() {
        given(inquiryService.canAccessInquiry(1L, 10L)).willReturn(true);

        ResponseEntity<ApiResponse<InquiryAccessResponse>> response = inquiryController.checkInquiryAccess(1L, 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().allowed()).isTrue();
        verify(inquiryService).canAccessInquiry(1L, 10L);
    }
}
