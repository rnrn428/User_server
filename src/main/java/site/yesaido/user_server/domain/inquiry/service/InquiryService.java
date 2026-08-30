package site.yesaido.user_server.domain.inquiry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCategoryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.entity.InquiryStatus;

import java.util.List;

public interface InquiryService {
    List<InquiryCategoryResponse> getCategories();
    InquiryDetailResponse createInquiry(Long userId, InquiryCreateRequest request, List<MultipartFile> files);
    Page<InquirySummaryResponse> getMyInquiries(Long userId, Pageable pageable);
    InquiryDetailResponse getMyInquiryDetail(Long userId, Long inquiryId);
    InquiryDetailResponse addFollowUp(Long userId, Long inquiryId, InquiryMessageRequest request, List<MultipartFile> files);

    // 관리자용 API
    Page<InquirySummaryResponse> getAllInquiries(Long adminUserId, InquiryStatus statusFilter, Pageable pageable);
    InquiryDetailResponse getInquiryDetailForAdmin(Long adminUserId, Long inquiryId);
    InquiryDetailResponse answerMessage(Long adminUserId, Long answerId, InquiryMessageRequest request, List<MultipartFile> files);

    InquiryCategoryResponse createCategory(Long adminId, InquiryCategoryCreateRequest request);

    boolean canAccessInquiry(Long userId, Long inquiryId);
}