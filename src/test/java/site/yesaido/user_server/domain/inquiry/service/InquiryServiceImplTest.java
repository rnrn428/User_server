package site.yesaido.user_server.domain.inquiry.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.inquiry.client.CultivationClient;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCategoryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryCreateRequest;
import site.yesaido.user_server.domain.inquiry.dto.request.InquiryMessageRequest;
import site.yesaido.user_server.domain.inquiry.dto.response.CultivationSummaryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryCategoryResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquiryDetailResponse;
import site.yesaido.user_server.domain.inquiry.dto.response.InquirySummaryResponse;
import site.yesaido.user_server.domain.inquiry.entity.Inquiry;
import site.yesaido.user_server.domain.inquiry.entity.InquiryAnswer;
import site.yesaido.user_server.domain.inquiry.entity.InquiryCategory;
import site.yesaido.user_server.domain.inquiry.entity.InquiryStatus;
import site.yesaido.user_server.domain.inquiry.exception.FileUploadException;
import site.yesaido.user_server.domain.inquiry.exception.InquiryAccessDeniedException;
import site.yesaido.user_server.domain.inquiry.exception.InquiryCategoryNotFoundException;
import site.yesaido.user_server.domain.inquiry.exception.InquiryPhotoLimitExceededException;
import site.yesaido.user_server.domain.inquiry.repository.InquiryAnswerRepository;
import site.yesaido.user_server.domain.inquiry.repository.InquiryCategoryRepository;
import site.yesaido.user_server.domain.inquiry.repository.InquiryPhotoRepository;
import site.yesaido.user_server.domain.inquiry.repository.InquiryRepository;
import site.yesaido.user_server.domain.inquiry.service.impl.InquiryServiceImpl;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.domain.user.service.MinioService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class InquiryServiceImplTest {
    @Mock
    private InquiryRepository inquiryRepository;
    @Mock
    private InquiryAnswerRepository inquiryAnswerRepository;

    @Mock
    private InquiryCategoryRepository inquiryCategoryRepository;

    @Mock
    private InquiryPhotoRepository inquiryPhotoRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CultivationClient cultivationClient;
    @Mock
    private MinioService minioService;
    @InjectMocks
    private InquiryServiceImpl inquiryService;

    // 1. 카테고리 조회
    @Test
    @DisplayName("카테고리 목록 조회 성공")
    void getCategories_success(){
        InquiryCategory category = InquiryCategory.create("재배 문의");
        given(inquiryCategoryRepository.findAll()).willReturn(List.of(category));

        List<InquiryCategoryResponse> responses = inquiryService.getCategories();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().categoryName()).isEqualTo("재배 문의");
    }

    // 2. 문의 생성
    @Nested
    @DisplayName("문의 생성 테스트")
    class CreateInquiryTest{
        @Test
        @DisplayName("성공 - 사진 없는 경우 & 경작지 정보 포함")
        void createInquiry_withoutFiles_success(){
            Long userId = 1L;
            InquiryCategory category = InquiryCategory.create("재배 문의");
            InquiryCreateRequest request = new InquiryCreateRequest(1L, "버섯 질문", "습도 조절 문의", 100L);
            Inquiry savedInquiry = Inquiry.builder().id(10L).userId(userId).category(category).title(request.getTitle()).cultivationId(100L).build();
            InquiryAnswer savedAnswer = InquiryAnswer.createRoot(savedInquiry, request.getContent());

            given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(inquiryRepository.save(any(Inquiry.class))).willReturn(savedInquiry);
            given(inquiryAnswerRepository.save(any(InquiryAnswer.class))).willReturn(savedAnswer);
            given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().id(userId).nickName("머쉬룸").build()));
            given(cultivationClient.getCultivation(userId, 100L)).willReturn(new CultivationSummaryResponse(100L, "1호 경작지", "CULTIVATION", "AUTO"));

            InquiryDetailResponse response = inquiryService.createInquiry(userId, request, null);

            assertThat(response.title()).isEqualTo("버섯 질문");
            assertThat(response.cultivationName()).isEqualTo("1호 경작지");
            assertThat(response.userNickname()).isEqualTo("머쉬룸");
            verify(minioService, never()).uploadInquiryPhoto(any(), any());
        }

        @Test
        @DisplayName("성공 - 사진 2장 첨부 시 MiniO 업로드 및 InquiryPhoto 저장")
        void createInquiry_withFiles_success(){
            Long userId = 1L;
            InquiryCategory category = InquiryCategory.create("재배 문의");
            InquiryCreateRequest request = new InquiryCreateRequest(1L, "사진 문의", "버섯 상태 확인", null);
            Inquiry savedInquiry = Inquiry.builder().id(10L).userId(userId).category(category).title(request.getTitle()).build();
            InquiryAnswer savedAnswer = InquiryAnswer.createRoot(savedInquiry, request.getContent());

            MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", "data1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", "data2".getBytes());
            List<MultipartFile> files = List.of(file1, file2);

            given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(inquiryRepository.save(any(Inquiry.class))).willReturn(savedInquiry);
            given(inquiryAnswerRepository.save(any(InquiryAnswer.class))).willReturn(savedAnswer);
            given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().id(userId).nickName("머쉬룸").build()));
            given(minioService.uploadInquiryPhoto(any(), eq(file1))).willReturn("inquiries/1/uuid1.jpg");
            given(minioService.uploadInquiryPhoto(any(), eq(file2))).willReturn("inquiries/2/uuid2.jpg");

            InquiryDetailResponse response = inquiryService.createInquiry(userId, request, files);

            assertThat(response).isNotNull();
            verify(minioService, times(2)).uploadInquiryPhoto(any(), any());
            verify(inquiryPhotoRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("예외 - 카테고리가 존재하지 않으면 InquiryCategoryNotFoundException")
        void createInquiry_categoryNotFound_throwsException(){
            InquiryCreateRequest request = new InquiryCreateRequest(1L, "제목", "내용", null);
            given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(()-> inquiryService.createInquiry(1L, request, null))
                    .isInstanceOf(InquiryCategoryNotFoundException.class);
        }

        @Test
        @DisplayName("예외 - 사진 5장 초과 시 InquiryPhotoLimitExceededException")
        void createInquiry_exceedMaxPhotos_throwsException(){
            Long userId = 1L;
            InquiryCategory category = InquiryCategory.create("재배 문의");
            InquiryCreateRequest request = new InquiryCreateRequest(1L, "제목", "내용", null);
            Inquiry savedInquiry = Inquiry.builder().id(10L).userId(userId).category(category).title(request.getTitle()).build();
            InquiryAnswer answer = InquiryAnswer.createRoot(savedInquiry, request.getContent());

            List<MultipartFile> sixFiles = List.of(
                    mock(MultipartFile.class), mock(MultipartFile.class), mock(MultipartFile.class),
                    mock(MultipartFile.class), mock(MultipartFile.class), mock(MultipartFile.class)
            );

            given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(inquiryRepository.save(any(Inquiry.class))).willReturn(savedInquiry);
            given(inquiryAnswerRepository.save(any(InquiryAnswer.class))).willReturn(answer);

            assertThatThrownBy(()-> inquiryService.createInquiry(userId, request, sixFiles))
                    .isInstanceOf(InquiryPhotoLimitExceededException.class);
        }

        @Test
        @DisplayName("예외 및 롤백 - 2번째 사진 업로드 실패 시 첫 번째 사진 자동 보상 삭제")
        void createInquiry_uploadFail_rollbackUploadedFiles(){
            Long userId = 1L;
            InquiryCategory category = InquiryCategory.create("재배 문의");
            InquiryCreateRequest request = new InquiryCreateRequest(1L, "제목", "내용", null);
            Inquiry savedInquiry = Inquiry.builder().id(10L).userId(userId).category(category).title(request.getTitle()).build();
            InquiryAnswer answer = InquiryAnswer.createRoot(savedInquiry, request.getContent());

            MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", "data1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", "data2".getBytes());
            List<MultipartFile> files = List.of(file1, file2);

            given(inquiryCategoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(inquiryRepository.save(any(Inquiry.class))).willReturn(savedInquiry);
            given(inquiryAnswerRepository.save(any(InquiryAnswer.class))).willReturn(answer);
            given(minioService.uploadInquiryPhoto(any(), eq(file1))).willReturn("inquiries/1/uuid1.jpg");
            given(minioService.uploadInquiryPhoto(any(), eq(file2))).willThrow(new FileUploadException("MiniO 통신 에러"));

            assertThatThrownBy(() -> inquiryService.createInquiry(userId, request, files))
                    .isInstanceOf(RuntimeException.class);

            verify(minioService).deleteFile("inquiries/1/uuid1.jpg");
        }

        // 3. 내 문의 목록 & 상세 조회
        @Nested
        @DisplayName("사용자 문의 조회 테스트")
        class UserInquiryQueryTest{
            @Test
            @DisplayName("내 문의 목록 페이징 조회 성공")
            void getMyInquiries_success(){
                Long userId = 1L;
                InquiryCategory category = InquiryCategory.create("일반");
                Inquiry inquiry = Inquiry.builder().id(1L).userId(userId).category(category).title("문의 제목").build();
                Page<Inquiry> inquiryPage = new PageImpl<>(List.of(inquiry), PageRequest.of(0, 10), 1);

                given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().id(userId).nickName("닉네임").build()));
                given(inquiryRepository.findAllByUserId(eq(userId), any())).willReturn(inquiryPage);

                Page<InquirySummaryResponse> responses = inquiryService.getMyInquiries(userId, PageRequest.of(0, 10));

                assertThat(responses.getContent()).hasSize(1);
                assertThat(responses.getContent().getFirst().title()).isEqualTo("문의 제목");

            }
            @Test
            @DisplayName("내 문의 상세 조회 성공")
            void getMyInquiryDetail_success() {
                Long userId = 1L;
                InquiryCategory category = InquiryCategory.create("일반");
                Inquiry inquiry = Inquiry.builder().id(10L).userId(userId).category(category).title("상세 제목").build();

                given(inquiryRepository.findById(10L)).willReturn(Optional.of(inquiry));
                given(inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(10L)).willReturn(List.of());
                given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().id(userId).nickName("닉네임").build()));

                InquiryDetailResponse response = inquiryService.getMyInquiryDetail(userId, 10L);

                assertThat(response.title()).isEqualTo("상세 제목");
            }

            @Test
            @DisplayName("예외 - 다른 유저의 문의 조회 시 InquiryAccessDeniedException")
            void getMyInquiryDetail_notOwner_throwsException() {
                Long userId = 1L;
                InquiryCategory category = InquiryCategory.create("일반");
                Inquiry otherUsersInquiry = Inquiry.builder().id(10L).userId(2L).category(category).title("남의 문의").build();

                given(inquiryRepository.findById(10L)).willReturn(Optional.of(otherUsersInquiry));

                assertThatThrownBy(() -> inquiryService.getMyInquiryDetail(userId, 10L))
                        .isInstanceOf(InquiryAccessDeniedException.class);

            }


            @Test
            @DisplayName("경작지 정보 조회 실패 시 (Feign 장애 등)에도 null을 반환하며 정상 조회된다.")
            void getMyInquiryDetail_cultivationClientError_returnNullName(){
                Long userId = 1L;
                InquiryCategory category = InquiryCategory.create("재배");
                Inquiry inquiry = Inquiry.builder()
                        .id(10L)
                        .userId(userId)
                        .category(category)
                        .cultivationId(100L)
                        .title("경작지 문의")
                        .build();

                given(inquiryRepository.findById(10L)).willReturn(Optional.of(inquiry));
                given(inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(10L)).willReturn(List.of());
                given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().id(userId).nickName("닉네임").build()));

                // Feign 호출 시 런타임 예외 발생 시뮬레이션
                given(cultivationClient.getCultivation(userId, 100L)).willThrow(new RuntimeException("Feign Timeout / Connection Error"));

                // 2. When
                InquiryDetailResponse response = inquiryService.getMyInquiryDetail(userId, 10L);

                // 3. Then
                assertThat(response).isNotNull();
                assertThat(response.cultivationName()).isNull(); // 에러가 나도 null로 처리되고 조회가 완료됨
            }
    }

        // ===== 4. 추가 메시지 (FollowUp) =====
        @Nested
        @DisplayName("추가 대화 전송 테스트")
        class FollowUpTest {

            @Test
            @DisplayName("추가 대화 작성 성공 - 상태가 PENDING으로 변경됨")
            void addFollowUp_success() {
                Long userId = 1L;
                InquiryCategory category = InquiryCategory.create("일반");
                Inquiry inquiry = Inquiry.builder().id(10L).userId(userId).category(category).title("대화 문의").build();
                InquiryAnswer rootMessage = InquiryAnswer.createRoot(inquiry, "첫 질문");
                InquiryMessageRequest request = new InquiryMessageRequest("추가 질문입니다.");

                given(inquiryRepository.findById(10L)).willReturn(Optional.of(inquiry));
                given(inquiryAnswerRepository.findTopByInquiryIdOrderByCreatedAtDesc(10L)).willReturn(Optional.of(rootMessage));
                given(inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(10L)).willReturn(List.of(rootMessage));
                given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().id(userId).nickName("닉네임").build()));

                InquiryDetailResponse response = inquiryService.addFollowUp(userId, 10L, request, null);

                assertThat(response).isNotNull();
                assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
                verify(inquiryAnswerRepository).save(any(InquiryAnswer.class));
            }
        }

        // ===== 5. 관리자 기능 (Admin) =====
        @Nested
        @DisplayName("관리자 기능 테스트")
        class AdminInquiryTest {

            @Test
            @DisplayName("관리자 카테고리 생성 성공")
            void createCategory_success() {
                Long adminId = 99L;
                User admin = User.builder().id(adminId).role(Role.ADMIN).build();
                InquiryCategory category = InquiryCategory.create("신규 카테고리");

                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
                given(inquiryCategoryRepository.save(any())).willReturn(category);

                InquiryCategoryResponse response = inquiryService.createCategory(adminId, new InquiryCategoryCreateRequest("신규 카테고리"));

                assertThat(response.categoryName()).isEqualTo("신규 카테고리");
            }

            @Test
            @DisplayName("예외 - 일반 유저가 관리자 카테고리 생성 시도 시 InquiryAccessDeniedException")
            void createCategory_notAdmin_throwsException() {
                Long userId = 1L;
                User normalUser = User.builder().id(userId).role(Role.USER).build();
                InquiryCategoryCreateRequest request = new InquiryCategoryCreateRequest("해당 카테고리");

                given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));

                assertThatThrownBy(() -> inquiryService.createCategory(userId, request))
                        .isInstanceOf(InquiryAccessDeniedException.class);
            }

            @Test
            @DisplayName("관리자 답변 작성 성공 - 상태가 RESOLVED로 변경됨")
            void answerMessage_success() {
                Long adminId = 99L;
                User admin = User.builder().id(adminId).role(Role.ADMIN).build();
                InquiryCategory category = InquiryCategory.create("일반");
                Inquiry inquiry = Inquiry.builder().id(10L).userId(1L).category(category).title("문의").build();
                InquiryAnswer answer = InquiryAnswer.createRoot(inquiry, "질문 내용");

                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
                given(inquiryAnswerRepository.findById(50L)).willReturn(Optional.of(answer));
                given(inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(10L)).willReturn(List.of(answer));
                given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().id(1L).nickName("질문자").build()));

                InquiryDetailResponse response = inquiryService.answerMessage(adminId, 50L, new InquiryMessageRequest("답변입니다."), null);

                assertThat(response).isNotNull();
                assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RESOLVED);
                assertThat(answer.getAnswerContent()).isEqualTo("답변입니다.");
            }

            @Test
            @DisplayName("관리자 전체 문의 목록 조회 성공 (상태 필터링)")
            void getAllInquiries_withStatus_success() {
                Long adminId = 99L;
                User admin = User.builder().id(adminId).role(Role.ADMIN).build();
                InquiryCategory category = InquiryCategory.create("일반");
                Inquiry inquiry = Inquiry.builder().id(1L).userId(1L).category(category).title("문의").build();
                Page<Inquiry> page = new PageImpl<>(List.of(inquiry));

                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
                given(inquiryRepository.findAllByStatus(eq(InquiryStatus.PENDING), any())).willReturn(page);
                given(userRepository.findAllById(any())).willReturn(List.of(User.builder().id(1L).nickName("유저1").build()));

                Page<InquirySummaryResponse> responses = inquiryService.getAllInquiries(adminId, InquiryStatus.PENDING, PageRequest.of(0,
                        10));

                assertThat(responses.getContent()).hasSize(1);
                assertThat(responses.getContent().getFirst().userNickname() ).isEqualTo("유저1");
            }

            @Test
            @DisplayName("관리자가 특정 문의 상세를 조회할 떄")
            void getInquiryDetailForAdmin_success(){
                Long adminId = 99L;
                User admin = User.builder().id(adminId).role(Role.ADMIN).build();
                InquiryCategory category = InquiryCategory.create("일반");
                Inquiry inquiry = Inquiry.builder().id(1L).userId(1L).category(category).title("관리자용 상세 문의").build();

                given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
                given(inquiryRepository.findById(10L)).willReturn(Optional.of(inquiry));
                given(inquiryAnswerRepository.findAllByInquiryIdOrderByCreatedAtAsc(10L)).willReturn(List.of());
                given(userRepository.findById(1L)).willReturn(Optional.of(User.builder().id(1L).nickName("질문자").build()));

                InquiryDetailResponse response = inquiryService.getInquiryDetailForAdmin(adminId, 10L);

                assertThat(response).isNotNull();
                assertThat(response.title()).isEqualTo("관리자용 상세 문의");
                assertThat(response.userNickname()).isEqualTo("질문자");
            }
        }
    }

    @Nested
    @DisplayName("문의 접근 권한 검증 테스트")
    class InquiryAccessTest {

        @Test
        @DisplayName("문의 작성자는 관리자 여부 조회 없이 접근이 허용된다")
        void canAccessInquiry_ownerReturnsTrue() {
            Long userId = 1L;
            Inquiry inquiry = Inquiry.builder().id(10L).userId(userId).title("내 문의").build();
            given(inquiryRepository.findById(10L)).willReturn(Optional.of(inquiry));

            boolean allowed = inquiryService.canAccessInquiry(userId, 10L);

            assertThat(allowed).isTrue();
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("관리자는 다른 사용자의 문의에도 접근이 허용된다")
        void canAccessInquiry_adminReturnsTrue() {
            User admin = User.builder().id(99L).role(Role.ADMIN).build();
            Inquiry inquiry = Inquiry.builder().id(10L).userId(1L).title("다른 사람 문의").build();
            given(inquiryRepository.findById(10L)).willReturn(Optional.of(inquiry));
            given(userRepository.findById(99L)).willReturn(Optional.of(admin));

            boolean allowed = inquiryService.canAccessInquiry(99L, 10L);

            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("문의 작성자도 관리자도 아니면 접근이 거절된다")
        void canAccessInquiry_nonOwnerUserReturnsFalse() {
            User user = User.builder().id(2L).role(Role.USER).build();
            Inquiry inquiry = Inquiry.builder().id(10L).userId(1L).title("다른 사람 문의").build();
            given(inquiryRepository.findById(10L)).willReturn(Optional.of(inquiry));
            given(userRepository.findById(2L)).willReturn(Optional.of(user));

            boolean allowed = inquiryService.canAccessInquiry(2L, 10L);

            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 문의는 접근이 거절된다")
        void canAccessInquiry_missingInquiryReturnsFalse() {
            given(inquiryRepository.findById(999L)).willReturn(Optional.empty());

            boolean allowed = inquiryService.canAccessInquiry(1L, 999L);

            assertThat(allowed).isFalse();
            verify(userRepository, never()).findById(any());
        }
    }

}



















