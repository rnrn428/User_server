package site.yesaido.user_server.domain.inquiry.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.client.CultivationClient;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCategoryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.CultivationSummaryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.entity.*;
import site.yesaido.user_server.domain.inquiry.exception.*;
import site.yesaido.user_server.domain.inquiry.repository.InquiryAnswerRepository;
import site.yesaido.user_server.domain.inquiry.repository.InquiryCategoryRepository;
import site.yesaido.user_server.domain.inquiry.repository.InquiryPhotoRepository;
import site.yesaido.user_server.domain.inquiry.repository.InquiryRepository;
import site.yesaido.user_server.domain.inquiry.service.InquiryService;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.exception.UserNotFoundException;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.domain.user.service.MinioService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {
    private static final int MAX_PHOTO_COUNT = 5;

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final InquiryCategoryRepository inquiryCategoryRepository;
    private final InquiryPhotoRepository inquiryPhotoRepository;
    private final UserRepository userRepository;
    private final CultivationClient cultivationClient;
    private final MinioService minioService;

    @Override
    public List<InquiryCategoryResponse> getCategories() {
        return inquiryCategoryRepository.findAll().stream()
                .map(InquiryCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public InquiryDetailResponse createInquiry(Long userId, InquiryCreateRequest request, List<MultipartFile> files) {
        InquiryCategory category = inquiryCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(InquiryCategoryNotFoundException::new);

        Inquiry inquiry = inquiryRepository.save(Inquiry.builder()
                .userId(userId)
                .category(category)
                .title(request.getTitle())
                .cultivationId(request.getCultivationId())
                .build());

        InquiryAnswer rootMessage = inquiryAnswerRepository.save(
                InquiryAnswer.createRoot(inquiry, request.getContent())
        );

        saveInquiryPhotos(rootMessage, files);

        return InquiryDetailResponse.of(inquiry, List.of(rootMessage), resolveCultivationName(inquiry), resolveUserNickname(userId));
    }

    public Page<InquirySummaryResponse> getMyInquiries(Long userId, Pageable pageable) {
        String nickname = resolveUserNickname(userId);
        return inquiryRepository.findAllByUserId(userId, pageable)
                .map(inquiry -> InquirySummaryResponse.from(inquiry, nickname));
    }

    public InquiryDetailResponse getMyInquiryDetail(Long userId, Long inquiryId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        requireOwner(inquiry, userId);

        List<InquiryAnswer> messages = inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(inquiryId);
        return InquiryDetailResponse.of(inquiry, messages, resolveCultivationName(inquiry), resolveUserNickname(userId));
    }

    @Override
    @Transactional
    public InquiryCategoryResponse createCategory(Long adminId, InquiryCategoryCreateRequest request) {
        requireAdmin(adminId);

        InquiryCategory category = InquiryCategory.create(request.categoryName());
        InquiryCategory savedCategory = inquiryCategoryRepository.save(category);

        return InquiryCategoryResponse.from(savedCategory);
    }

    @Override
    @Transactional
    public InquiryDetailResponse addFollowUp(Long userId, Long inquiryId, InquiryMessageRequest request) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        requireOwner(inquiry, userId);

        InquiryAnswer latest = inquiryAnswerRepository.findTopByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .orElseThrow(InquiryAnswerNotFoundException::new);

        inquiryAnswerRepository.save(InquiryAnswer.createFollowUp(inquiry, latest, request.getContent()));

        inquiry.markPending();

        List<InquiryAnswer> messages = inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(inquiryId);
        return InquiryDetailResponse.of(inquiry, messages, resolveCultivationName(inquiry), resolveUserNickname(userId));
    }

    // 관리자 용
    @Override
    public Page<InquirySummaryResponse> getAllInquiries(Long adminUserId, InquiryStatus statusFilter, Pageable pageable) {
        requireAdmin(adminUserId);

        Page<Inquiry> page = statusFilter != null
                ? inquiryRepository.findAllByStatus(statusFilter, pageable)
                : inquiryRepository.findAll(pageable);

        List<Long> userIds = page.getContent().stream().map(Inquiry::getUserId).distinct().toList();
        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickName));

        return page.map(inquiry -> InquirySummaryResponse.from(inquiry, nicknameMap.getOrDefault(inquiry.getUserId(), "알 수 없음")));
    }

    @Override
    public InquiryDetailResponse getInquiryDetailForAdmin(Long adminUserId, Long inquiryId) {
        requireAdmin(adminUserId);

        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        List<InquiryAnswer> messages = inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(inquiryId);
        return InquiryDetailResponse.of(inquiry, messages, resolveCultivationName(inquiry), resolveUserNickname(inquiry.getUserId()));
    }

    @Override
    @Transactional
    public InquiryDetailResponse answerMessage(Long adminUserId, Long answerId, InquiryMessageRequest request) {
        requireAdmin(adminUserId);

        InquiryAnswer message = inquiryAnswerRepository.findById(answerId)
                .orElseThrow(InquiryAnswerNotFoundException::new);

        message.answer(request.getContent());

        Inquiry inquiry = message.getInquiry();
        inquiry.markResolved();

        List<InquiryAnswer> messages = inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(inquiry.getId());
        return InquiryDetailResponse.of(inquiry, messages, resolveCultivationName(inquiry), resolveUserNickname(inquiry.getUserId()));
    }

    // Helper Method
    private void requireAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != Role.ADMIN) {
            throw new InquiryAccessDeniedException("관리자만 접근할 수 있습니다.");
        }
    }

    private void requireOwner(Inquiry inquiry, Long userId) {
        if (!inquiry.getUserId().equals(userId)) {
            throw new InquiryAccessDeniedException();
        }
    }

    private Inquiry getInquiryOrThrow(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(InquiryNotFoundException::new);
    }

    private String resolveCultivationName(Inquiry inquiry) {
        if (inquiry.getCultivationId() == null) {
            return null;
        }
        try {
            CultivationSummaryResponse cultivation = cultivationClient.getCultivation(inquiry.getUserId(), inquiry.getCultivationId());
            return cultivation != null ? cultivation.name() : null;
        } catch (Exception e) {
            log.warn("경작지 정보 조회 실패 (inquiryId={}, cultivationId={}): {}", inquiry.getId(), inquiry.getCultivationId(), e.getMessage());
            return null;
        }
    }

    private void saveInquiryPhotos(InquiryAnswer inquiryAnswer, List<MultipartFile> files){
        if(files == null || files.isEmpty()){
            return;
        }

        if(files.size() > MAX_PHOTO_COUNT){
            throw new InquiryPhotoLimitExceededException("문의 사진은 최대 " + MAX_PHOTO_COUNT + "장까지만 등록할 수 있습니다.");
        }

        List<String> uploadKeys = new ArrayList<>();

        try{
            for(MultipartFile file : files){
                String objectKey = minioService.uploadInquiryPhoto(inquiryAnswer.getId(), file);
                uploadKeys.add(objectKey);

                InquiryPhoto inquiryPhoto = InquiryPhoto.create(inquiryAnswer, objectKey);
                inquiryPhotoRepository.save(inquiryPhoto);
            }
        }catch (Exception e){
            for(String key : uploadKeys){
                try{
                    minioService.deleteFile(key);
                }catch (Exception deleteEx){
                    log.error("MiniO 보상 삭제 실패 : key={}", key, deleteEx);
                }
            }
            throw e;
        }

    }

    @Override
    public boolean canAccessInquiry(Long userId, Long inquiryId) {
        return inquiryRepository.findById(inquiryId).map(inquiry -> inquiry.getUserId().equals(userId) || isAdmin(userId)).orElse(false);
    }

    private boolean isAdmin(Long userId){
        return userRepository.findById(userId).map(user -> user.getRole() == Role.ADMIN).orElse(false);
    }

    private String resolveUserNickname(Long userId) {
        return userRepository.findById(userId).map(User::getNickName).orElse("알 수 없음");
    }
}