package site.yesaido.user_server.domain.user.service;

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
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import site.yesaido.user_server.domain.email.service.EmailService;
import site.yesaido.user_server.domain.user.dto.MemberSummaryResponse;
import site.yesaido.user_server.domain.user.dto.UserSummaryResponse;
import site.yesaido.user_server.domain.user.dto.profile.PasswordChangeRequest;
import site.yesaido.user_server.domain.user.dto.profile.ProfileUpdateRequest;
import site.yesaido.user_server.domain.user.dto.profile.UserProfileResponse;
import site.yesaido.user_server.domain.user.dto.search.UserSearchResponse;
import site.yesaido.user_server.domain.user.dto.signup.SignupEligibility;
import site.yesaido.user_server.domain.user.dto.signup.UserSignResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignUpRequest;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.*;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MinioService minioService;

    @Mock
    private site.yesaido.user_server.domain.user.repository.ProfileImageRepository profileImageRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("회원가입 기능 테스트")
    class SingUpTest{
        @Test
        @DisplayName("성공 : 올바른 회원가입 요청 시 비밀번호가 암호화되어 저장된다")
        void success_signUp(){
            UserSignUpRequest requestDto = UserSignUpRequest.builder()
                    .email("rnrn428@naver.com")
                    .password("password123!")
                    .nickName("duplicate")
                    .role(Role.USER)
                    .build();

            User savedUser = User.builder()
                            .email("rnrn428@naver.com")
                                    .password("$2a$10$encodedPassword")
                                            .nickName("duplicate")
                                                    .status(UserStatus.ACTIVE)
                                                            .role(Role.USER)
                                                                    .build();

            given(emailService.isSignupEmailVerified(requestDto.getEmail())).willReturn(true);
            given(userRepository.findByEmail(requestDto.getEmail())).willReturn(Optional.empty());
            given(userRepository.existsByNickName(requestDto.getNickName())).willReturn(false);
            given(passwordEncoder.encode(requestDto.getPassword())).willReturn("$2a$10$encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            UserSignResponse responseDto = userService.signUp(requestDto, null);

            assertThat(responseDto).isNotNull();
            assertThat(responseDto.getEmail()).isEqualTo("rnrn428@naver.com");
            assertThat(responseDto.getNickName()).isEqualTo("duplicate");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("프로필 사진을 포함해 가입하면 MinIO와 프로필 이미지 정보를 저장한다")
        void success_signUpWithProfileImage() {
            UserSignUpRequest requestDto = UserSignUpRequest.builder()
                    .email("image@test.com")
                    .password("password123!")
                    .nickName("image-user")
                    .role(Role.USER)
                    .build();
            User savedUser = User.builder()
                    .email("image@test.com")
                    .password("encoded-password")
                    .nickName("image-user")
                    .status(UserStatus.ACTIVE)
                    .role(Role.USER)
                    .build();
            ReflectionTestUtils.setField(savedUser, "id", 1L);
            MockMultipartFile profileImage = new MockMultipartFile(
                    "profileImage", "profile.png", "image/png", "image-content".getBytes()
            );

            given(emailService.isSignupEmailVerified(requestDto.getEmail())).willReturn(true);
            given(userRepository.findByEmail(requestDto.getEmail())).willReturn(Optional.empty());
            given(userRepository.existsByNickName(requestDto.getNickName())).willReturn(false);
            given(passwordEncoder.encode(requestDto.getPassword())).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(minioService.uploadProfileImage(1L, profileImage)).willReturn("profiles/1/profile.png");

            TransactionSynchronizationManager.initSynchronization();
            try {
                userService.signUp(requestDto, profileImage);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }

            verify(minioService).uploadProfileImage(1L, profileImage);
            verify(profileImageRepository).save(any());
            verify(emailService).clearSignupEmailVerification(requestDto.getEmail());
        }

        @Test
        @DisplayName("실패 : 이미 존재하는 이메일로 가입 시 예외가 발생")
        void signUp_DuplicateEmail(){
            UserSignUpRequest requestDto = UserSignUpRequest.builder()
                    .email("rnrn428@naver.com")
                    .password("password123!")
                    .nickName("duplicate_fail")
                    .role(Role.USER)
                    .build();

            User existingUser = User.builder().email(requestDto.getEmail()).status(UserStatus.ACTIVE).build();
            given(emailService.isSignupEmailVerified(requestDto.getEmail())).willReturn(true);
            given(userRepository.findByEmail(requestDto.getEmail())).willReturn(Optional.of(existingUser));

            assertThrows(EmailDuplicationException.class, () -> userService.signUp(requestDto, null));

            verify(userRepository, never()).save(any(User.class));

        }

        @Test
        @DisplayName("실패 : 이미 존재하는 닉네임으로 가입 시 예외 발생")
        void signUp_DuplicateNickname(){
            UserSignUpRequest requestDto = UserSignUpRequest.builder()
                    .email("rnrn428@naver.com")
                    .password("password123!")
                    .nickName("duplicate_fail")
                    .role(Role.USER)
                    .build();

            given(emailService.isSignupEmailVerified(requestDto.getEmail())).willReturn(true);
            given(userRepository.findByEmail(requestDto.getEmail())).willReturn(Optional.empty());
            given(userRepository.existsByNickName(requestDto.getNickName())).willReturn(true);

            assertThrows(NicknameDuplicationException.class, () -> userService.signUp(requestDto, null));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("실패 : 이메일 인증 없이 가입하면 예외가 발생한다")
        void signUp_withoutEmailVerification() {
            UserSignUpRequest requestDto = UserSignUpRequest.builder()
                    .email("new@test.com")
                    .password("password123!")
                    .nickName("newNick")
                    .build();

            given(emailService.isSignupEmailVerified(requestDto.getEmail())).willReturn(false);

            assertThatThrownBy(() -> userService.signUp(requestDto, null))
                    .isInstanceOf(EmailVerificationRequiredException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("성공 : 인증을 마친 신규 이메일은 가입 가능 상태를 반환한다")
        void verifySignupEmail_newEmailAvailable() {
            String email = "new@test.com";
            given(emailService.verifySignupCode(email, "123456")).willReturn(true);
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            var response = userService.verifySignupEmail(email, "123456");

            assertThat(response.verified()).isTrue();
            assertThat(response.eligibility()).isEqualTo(SignupEligibility.AVAILABLE);
        }

        @Test
        @DisplayName("성공 : 휴면 상태인 이메일은 DORMANT 상태를 반환한다")
        void verifySignupEmail_dormantUserReturnsDormantEligibility() {
            String email = "dormant@test.com";
            User dormantUser = User.builder()
                    .email(email)
                    .status(UserStatus.DORMANT)
                    .build();
            given(emailService.verifySignupCode(email, "123456")).willReturn(true);
            given(userRepository.findByEmail(email)).willReturn(Optional.of(dormantUser));

            var response = userService.verifySignupEmail(email, "123456");

            assertThat(response.verified()).isTrue();
            assertThat(response.eligibility()).isEqualTo(SignupEligibility.DORMANT);
        }

        @Test
        @DisplayName("성공 : 탈퇴 후 30일 이내 이메일은 재가입 제한 상태를 반환한다")
        void verifySignupEmail_recentlyWithdrawnIsRestricted() {
            String email = "withdrawn@test.com";
            User withdrawnUser = User.builder()
                    .email(email)
                    .status(UserStatus.DELETED)
                    .deletedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(1))
                    .build();
            given(emailService.verifySignupCode(email, "123456")).willReturn(true);
            given(userRepository.findByEmail(email)).willReturn(Optional.of(withdrawnUser));

            var response = userService.verifySignupEmail(email, "123456");

            assertThat(response.verified()).isTrue();
            assertThat(response.eligibility()).isEqualTo(SignupEligibility.REJOIN_RESTRICTED);
            assertThat(response.rejoinAvailableAt()).isNotNull();
        }

        @Test
        @DisplayName("성공 : 탈퇴 후 30일이 지난 이메일은 기존 식별자를 익명화하고 새 계정을 만든다")
        void signUp_afterRejoinRestrictionCreatesNewUser() {
            UserSignUpRequest requestDto = UserSignUpRequest.builder()
                    .email("withdrawn@test.com")
                    .password("password123!")
                    .nickName("newNick")
                    .build();
            User withdrawnUser = User.builder()
                    .id(1L)
                    .email(requestDto.getEmail())
                    .nickName("oldNick")
                    .password("oldPassword")
                    .status(UserStatus.DELETED)
                    .deletedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(31))
                    .build();
            User newUser = User.builder()
                    .id(2L)
                    .email(requestDto.getEmail())
                    .nickName(requestDto.getNickName())
                    .status(UserStatus.ACTIVE)
                    .build();
            given(emailService.isSignupEmailVerified(requestDto.getEmail())).willReturn(true);
            given(userRepository.findByEmail(requestDto.getEmail())).willReturn(Optional.of(withdrawnUser));
            given(userRepository.existsByNickName(requestDto.getNickName())).willReturn(false);
            given(passwordEncoder.encode(requestDto.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(newUser);

            UserSignResponse response = userService.signUp(requestDto, null);

            assertThat(response.getId()).isEqualTo(2L);
            assertThat(withdrawnUser.getEmail()).startsWith("deleted-1-");
            assertThat(withdrawnUser.getNickName()).startsWith("deleted-1-");
            assertThat(withdrawnUser.getPassword()).isNull();
            verify(emailService).clearSignupEmailVerification(requestDto.getEmail());
        }
    }

    @Nested
    @DisplayName("유저 조회 테스트")
    class getUserByIdTest{
        @Test
        @DisplayName("성공 : ID로 존재하는 유저 조회")
        void success_getUserById(){
            User user = User.builder()
                    .id(1L)
                    .email("rnrn428@naver.com")
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            User foundUser = userService.getUserById(1L);

            assertThat(foundUser).isNotNull();
            assertThat(foundUser.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 유저 조회 시 예외 발생")
        void getUserById_UserNotFound(){
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, ()-> userService.getUserById(99L));

        }

        @Test
        @DisplayName("실패 : 이미 탈퇴한 유저 조회 시 예외 발생")
        void getUserById_AlreadyWithdrawn(){
            User deletedUser = User.builder()
                    .id(1L)
                    .status(UserStatus.DELETED).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(deletedUser));

            assertThrows(AlreadyWithdrawnException.class, () -> userService.getUserById(1L));
        }
    }

    @Nested
    @DisplayName("프로필 수정 테스트 그룹")
    class ProfileTest{
        @Test
        @DisplayName("성공 : 내 프로필 정상 조회")
        void getMyProfile_success(){
            User user = User.builder()
                    .id(1L)
                    .email("test@test.com")
                    .nickName("oldNick")
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            UserProfileResponse response = userService.getMyProfile(1L);

            assertThat(response).isNotNull();
            assertThat(response.nickname()).isEqualTo("oldNick");
            assertThat(response.hasPassword()).isFalse();
        }

        @Test
        @DisplayName("닉네임 변경 성공")
        void updateNickname_success(){
            User updateUser = User.builder()
                    .id(1L)
                    .nickName("oldNick")
                    .build();

            ProfileUpdateRequest request = new ProfileUpdateRequest("newNick");

            given(userRepository.findById(1L)).willReturn(Optional.of(updateUser));
            given(userRepository.existsByNickName("newNick")).willReturn(false);

            userService.updateProfile(1L, request);

            assertThat(updateUser.getNickName()).isEqualTo("newNick");

        }

        @Test
        @DisplayName("변경하려는 닉네임이 이미 존재할 시 예외 발생")
        void updateNickname_failed(){
            User updateUser = User.builder()
                    .id(1L)
                    .nickName("oldNick")
                    .build();

            ProfileUpdateRequest request = new ProfileUpdateRequest("newNick");


            given(userRepository.findById(1L)).willReturn(Optional.of(updateUser));
            given(userRepository.existsByNickName("newNick")).willReturn(true);

            assertThrows(NicknameDuplicationException.class, () -> userService.updateProfile(1L, request));
        }

        @Test
        @DisplayName("성공 : 입력한 비밀번호가 DB와 일치하면 true 반환")
        void verifyPassword_success(){
            User user = User.builder()
                    .id(1L)
                    .password("encodedPassword")
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("rawPass", "encodedPassword")).willReturn(true);

            boolean result = userService.verifyPassword(1L, "rawPass");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실패 : 입력한 비밀번호가 DB와 불일치하면 false 반환")
        void verifyPassword_failure(){
            User user = User.builder()
                    .id(1L)
                    .password("encodedPassword")
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("rawPass", "encodedPassword")).willReturn(false);

            boolean result = userService.verifyPassword(1L, "rawPass");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("비밀번호 변경 성공 시 비밀번호를 암호화하고 모든 Refresh Token을 폐기한다")
        void changePassword_success() {
            User user = User.builder().id(1L).nickName("nick").password("encodedOld").build();
            PasswordChangeRequest request = new PasswordChangeRequest("rawOld", "rawNew1!");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("rawOld", "encodedOld")).willReturn(true);
            given(passwordEncoder.matches("rawNew1!", "encodedOld")).willReturn(false);
            given(passwordEncoder.encode("rawNew1!")).willReturn("encodedNew");

            userService.changePassword(1L, request);

            assertThat(user.getPassword()).isEqualTo("encodedNew");
            verify(refreshTokenService).revokeAllRefreshTokens(1L);
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치 시 비밀번호를 변경하거나 Refresh Token을 폐기하지 않는다")
        void changePassword_wrongCurrentPassword_throwsException() {
            User user = User.builder().id(1L).nickName("nick").password("encodedOld").build();
            PasswordChangeRequest request = new PasswordChangeRequest("wrongOld", "rawNew1!");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongOld", "encodedOld")).willReturn(false);

            assertThrows(InvalidPasswordException.class,
                    () -> userService.changePassword(1L, request));
            assertThat(user.getPassword()).isEqualTo("encodedOld");
            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 기존 비밀번호와 같으면 변경하지 않는다")
        void changePassword_sameAsOld_throwsException() {
            User user = User.builder().id(1L).nickName("nick").password("encodedOld").build();
            PasswordChangeRequest request = new PasswordChangeRequest("rawOld", "rawOld");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("rawOld", "encodedOld")).willReturn(true);

            assertThrows(InvalidPasswordException.class,
                    () -> userService.changePassword(1L, request));
            verify(passwordEncoder, never()).encode(any());
            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }

        @Test
        @DisplayName("소셜 로그인 계정은 비밀번호를 변경할 수 없다")
        void changePassword_socialLoginUser_throwsException() {
            User user = User.builder().id(1L).nickName("social").password(null).build();
            PasswordChangeRequest request = new PasswordChangeRequest("rawOld", "rawNew1!");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThrows(InvalidPasswordException.class,
                    () -> userService.changePassword(1L, request));
            verify(passwordEncoder, never()).matches(any(), any());
            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }

        @Test
        @DisplayName("프로필 이미지 업로드 성공 - 기존 이미지가 없는 경우 새로 생성")
        void uploadProfileImage_newImage_success() {
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
            try {
                User user = User.builder().id(1L).build();
                org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);

                given(userRepository.findById(1L)).willReturn(Optional.of(user));
                given(minioService.uploadProfileImage(1L, file)).willReturn("profiles/1/new.jpg");
                given(profileImageRepository.findByUserId(1L)).willReturn(Optional.empty());
                given(minioService.presignedGetUrl("profiles/1/new.jpg")).willReturn("https://cdn.example.com/profiles/1/new.jpg");

                String result = userService.uploadProfileImage(1L, file);

                assertThat(result).isEqualTo("https://cdn.example.com/profiles/1/new.jpg");
                verify(profileImageRepository).save(any());
            } finally {
                org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("프로필 이미지 업로드 성공 - 기존 이미지가 있는 경우 키 교체")
        void uploadProfileImage_replaceImage_success() {
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
            try {
                User user = User.builder().id(1L).build();
                site.yesaido.user_server.domain.user.entity.ProfileImage oldImage =
                        site.yesaido.user_server.domain.user.entity.ProfileImage.create(user, "profiles/1/old.jpg");
                org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);

                given(userRepository.findById(1L)).willReturn(Optional.of(user));
                given(minioService.uploadProfileImage(1L, file)).willReturn("profiles/1/new.jpg");
                given(profileImageRepository.findByUserId(1L)).willReturn(Optional.of(oldImage));
                given(minioService.presignedGetUrl("profiles/1/new.jpg")).willReturn("https://cdn.example.com/profiles/1/new.jpg");

                String result = userService.uploadProfileImage(1L, file);

                assertThat(result).isEqualTo("https://cdn.example.com/profiles/1/new.jpg");
                assertThat(oldImage.getObjectKey()).isEqualTo("profiles/1/new.jpg");
            } finally {
                org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("프로필 이미지 업로드 실패 - 예외 발생 시 MinIO 업로드된 새 이미지 삭제")
        void uploadProfileImage_exception_deletesQuietly() {
            User user = User.builder().id(1L).build();
            org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(minioService.uploadProfileImage(1L, file)).willReturn("profiles/1/new.jpg");
            given(profileImageRepository.findByUserId(1L)).willThrow(new RuntimeException("DB 에러"));

            assertThrows(RuntimeException.class, () -> userService.uploadProfileImage(1L, file));
            verify(minioService).deleteQuietly("profiles/1/new.jpg");
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class WithdrawTest {
        @Test
        @DisplayName("올바른 비밀번호로 탈퇴하면 Soft Delete 처리하고 모든 Refresh Token을 폐기한다")
        void withdrawWithValidPassword_softDeletesAndRevokesRefreshTokens() {
            User user = User.builder()
                    .id(1L)
                    .email("rnrn428@naver.com")
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("raw-password", "encoded-password")).willReturn(true);

            userService.withdraw(1L, "raw-password");

            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(user.getDeletedAt()).isNotNull();
            verify(refreshTokenService).revokeAllRefreshTokens(1L);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 탈퇴하지 않고 Refresh Token도 유지한다")
        void withdrawWithWrongPassword_throwsException() {
            User user = User.builder()
                    .id(1L)
                    .password("encoded-password")
                    .status(UserStatus.ACTIVE)
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

            assertThatThrownBy(() -> userService.withdraw(1L, "wrong-password"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다.");

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }

        @Test
        @DisplayName("소셜 로그인 계정은 비밀번호 방식으로 탈퇴할 수 없다")
        void withdrawSocialLoginUser_throwsException() {
            User socialUser = User.builder()
                    .id(1L)
                    .password(null)
                    .status(UserStatus.ACTIVE)
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(socialUser));

            assertThatThrownBy(() -> userService.withdraw(1L, "any-password"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("소셜 로그인 계정은 비밀번호로 탈퇴할 수 없습니다.");

            verify(passwordEncoder, never()).matches(any(), any());
            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }

        @Test
        @DisplayName("Google 전용 계정은 현재 로그인 인증으로 탈퇴하고 모든 Refresh Token을 폐기한다")
        void withdrawOAuth_softDeletesAndRevokesRefreshTokens() {
            User user = User.builder()
                    .id(1L)
                    .password(null)
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            userService.withdrawOAuth(1L);

            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            verify(refreshTokenService).revokeAllRefreshTokens(1L);
        }

        @Test
        @DisplayName("비밀번호가 있는 계정은 OAuth 탈퇴 경로를 사용할 수 없다")
        void withdrawOAuth_rejectsAccountWithPassword() {
            User user = User.builder().id(1L).password("encoded-password").status(UserStatus.ACTIVE).build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.withdrawOAuth(1L))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("비밀번호가 설정된 계정은 비밀번호로 탈퇴해 주세요.");

            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }

        @Test
        @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
        void withdrawAlreadyDeletedUser_throwsException() {
            User user = User.builder()
                    .id(1L)
                    .password("encoded-password")
                    .status(UserStatus.DELETED)
                    .build();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.withdraw(1L, "raw-password"))
                    .isInstanceOf(AlreadyWithdrawnException.class);

            verify(passwordEncoder, never()).matches(any(), any());
            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }
    }

    @Test
    @DisplayName("닉네임이 존재하면 true 반환")
    void existNickname_true(){
        given(userRepository.existsByNickName("닉네임")).willReturn(true);

        boolean result = userService.existNickname("닉네임");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("닉네임이 존재하지 않으면 false 반환")
    void existNickname_false(){
        given(userRepository.existsByNickName("새닉네임")).willReturn(false);

        boolean result = userService.existNickname("새닉네임");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("키워드가 비어있으면 빈 리스트를 반환하고 저장소를 호출하지 않는다")
    void searchUsers_blankKeyword(){
        List<UserSearchResponse> result = userService.searchUsers("   ");

        assertThat(result).isEmpty();
        verify(userRepository, never()).searchActiveUsers(any(), any());
    }

    @Test
    @DisplayName("키워드로 활성 사용자를 검색해서 반환한다")
    void searchUsers_success(){
        User user = User.builder()
                .id(1L)
                .nickName("닉네임")
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.searchActiveUsers("닉네임", UserStatus.DELETED))
                .willReturn(List.of(user));

        List<UserSearchResponse> result = userService.searchUsers("닉네임");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().nickname()).isEqualTo("닉네임");
    }

    @Test
    @DisplayName("ID 목록으로 사용자 요약 정보를 조회한다")
    void getUsers_success(){
        User user1 = User.builder().id(1L).nickName("닉네임1").build();
        User user2 = User.builder().id(2L).nickName("닉네임2").build();

        given(userRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(user1, user2));

        List<UserSummaryResponse> result = userService.getUsers(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().nickname()).isEqualTo("닉네임1");
    }

    @Nested
    @DisplayName("관리자 회원 목록 조회 테스트")
    class getMembersTest {

        @Test
        @DisplayName("성공 : status=active면 활성 회원만 조회한다")
        void getMembers_active_success() {
            Long adminId = 99L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User member = User.builder().id(1L).nickName("닉네임").email("nick@test.com").status(UserStatus.ACTIVE).build();
            Pageable pageable = PageRequest.of(0, 8);
            Page<User> page = new PageImpl<>(List.of(member));

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findAllByStatus(UserStatus.ACTIVE, pageable)).willReturn(page);

            Page<MemberSummaryResponse> result = userService.getMembers(adminId, "active", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().nickname()).isEqualTo("닉네임");
            verify(userRepository, never()).findAllByStatusNot(any(), any());
        }

        @Test
        @DisplayName("성공 : status=dormant면 휴면 회원만 조회한다")
        void getMembers_dormant_success() {
            Long adminId = 99L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User dormantMember = User.builder().id(3L).nickName("휴면닉네임").status(UserStatus.DORMANT).build();
            Pageable pageable = PageRequest.of(0, 8);
            Page<User> page = new PageImpl<>(List.of(dormantMember));

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findAllByStatus(UserStatus.DORMANT, pageable)).willReturn(page);

            Page<MemberSummaryResponse> result = userService.getMembers(adminId, "dormant", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().nickname()).isEqualTo("휴면닉네임");
        }

        @Test
        @DisplayName("성공 : status=withdrawn이면 탈퇴 회원만 조회한다")
        void getMembers_withdrawn_success() {
            Long adminId = 99L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User withdrawnMember = User.builder().id(2L).nickName("탈퇴닉네임").status(UserStatus.DELETED).build();
            Pageable pageable = PageRequest.of(0, 8);
            Page<User> page = new PageImpl<>(List.of(withdrawnMember));

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findAllByStatus(UserStatus.DELETED, pageable)).willReturn(page);

            Page<MemberSummaryResponse> result = userService.getMembers(adminId, "withdrawn", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().nickname()).isEqualTo("탈퇴닉네임");
            verify(userRepository, never()).findAllByStatusNot(any(), any());
        }

        @Test
        @DisplayName("예외 - 일반 유저가 회원 목록 조회 시도 시 UserAccessDeniedException")
        void getMembers_notAdmin_throwsException() {
            Long userId = 1L;
            User normalUser = User.builder().id(userId).role(Role.USER).build();
            Pageable pageable = PageRequest.of(0, 8);

            given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));

            assertThatThrownBy(() -> userService.getMembers(userId, "active", pageable))
                    .isInstanceOf(UserAccessDeniedException.class);

            verify(userRepository, never()).findAllByStatusNot(any(), any());
        }

        @Test
        @DisplayName("예외 - 존재하지 않는 관리자 ID로 조회 시 UserNotFoundException")
        void getMembers_adminNotFound_throwsException() {
            Long adminId = 999L;
            Pageable pageable = PageRequest.of(0, 8);

            given(userRepository.findById(adminId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMembers(adminId, "active", pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("예외 - 지원하지 않는 회원 상태 필터는 InvalidMemberStatusException을 던진다")
        void getMembers_invalidStatusFilter_throwsException() {
            Long adminId = 99L;
            Pageable pageable = PageRequest.of(0, 8);
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));

            assertThatThrownBy(() -> userService.getMembers(adminId, "invalid", pageable))
                    .isInstanceOf(InvalidMemberStatusException.class)
                    .hasMessage("지원하지 않는 회원 상태입니다.");

            verify(userRepository, never()).findAllByStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("관리자 휴면 회원 해제 테스트")
    class ReleaseDormantMemberTest {

        @Test
        @DisplayName("성공 : 관리자가 휴면 회원을 활성 상태로 해제한다")
        void releaseDormantMember_success() {
            Long adminId = 99L;
            Long memberId = 1L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User dormantMember = User.builder().id(memberId).status(UserStatus.DORMANT).build();

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findById(memberId)).willReturn(Optional.of(dormantMember));

            userService.releaseDormantMember(adminId, memberId);

            assertThat(dormantMember.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("실패 : 휴면 상태가 아닌 회원은 해제할 수 없다")
        void releaseDormantMember_notDormant_throwsException() {
            Long adminId = 99L;
            Long memberId = 1L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User activeMember = User.builder().id(memberId).status(UserStatus.ACTIVE).build();

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findById(memberId)).willReturn(Optional.of(activeMember));

            assertThatThrownBy(() -> userService.releaseDormantMember(adminId, memberId))
                    .isInstanceOf(InvalidMemberStatusException.class)
                    .hasMessage("휴면 상태의 회원만 해제할 수 있습니다.");

            assertThat(activeMember.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("실패 : 일반 사용자는 휴면 회원을 해제할 수 없다")
        void releaseDormantMember_notAdmin_throwsException() {
            Long userId = 1L;
            User normalUser = User.builder().id(userId).role(Role.USER).build();

            given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));

            assertThatThrownBy(() -> userService.releaseDormantMember(userId, 2L))
                    .isInstanceOf(UserAccessDeniedException.class);

            verify(userRepository, never()).findById(2L);
        }
    }

    @Nested
    @DisplayName("관리자 강제 탈퇴 테스트")
    class ForceWithdrawTest {

        @Test
        @DisplayName("성공 : 관리자가 활성 회원을 강제 탈퇴하고 Refresh Token을 삭제한다")
        void forceWithdraw_activeMember_success() {
            Long adminId = 99L;
            Long memberId = 1L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User activeMember = User.builder().id(memberId).role(Role.USER).status(UserStatus.ACTIVE).build();

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findById(memberId)).willReturn(Optional.of(activeMember));

            userService.forceWithdraw(adminId, memberId);

            assertThat(activeMember.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(activeMember.getDeletedAt()).isNotNull();
            verify(refreshTokenService).revokeAllRefreshTokens(memberId);
        }

        @Test
        @DisplayName("성공 : 관리자가 휴면 회원도 강제 탈퇴할 수 있다")
        void forceWithdraw_dormantMember_success() {
            Long adminId = 99L;
            Long memberId = 1L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User dormantMember = User.builder().id(memberId).role(Role.USER).status(UserStatus.DORMANT).build();

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findById(memberId)).willReturn(Optional.of(dormantMember));

            userService.forceWithdraw(adminId, memberId);

            assertThat(dormantMember.getStatus()).isEqualTo(UserStatus.DELETED);
            verify(refreshTokenService).revokeAllRefreshTokens(memberId);
        }

        @Test
        @DisplayName("실패 : 관리자 계정은 강제 탈퇴할 수 없다")
        void forceWithdraw_adminMember_throwsException() {
            Long adminId = 99L;
            Long memberId = 100L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User targetAdmin = User.builder().id(memberId).role(Role.ADMIN).status(UserStatus.ACTIVE).build();

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findById(memberId)).willReturn(Optional.of(targetAdmin));

            assertThatThrownBy(() -> userService.forceWithdraw(adminId, memberId))
                    .isInstanceOf(InvalidForceWithdrawalException.class)
                    .hasMessage("관리자 계정은 강제 탈퇴할 수 없습니다.");

            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }

        @Test
        @DisplayName("실패 : 이미 탈퇴한 회원은 강제 탈퇴할 수 없다")
        void forceWithdraw_withdrawnMember_throwsException() {
            Long adminId = 99L;
            Long memberId = 1L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User withdrawnMember = User.builder().id(memberId).role(Role.USER).status(UserStatus.DELETED).build();

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findById(memberId)).willReturn(Optional.of(withdrawnMember));

            assertThatThrownBy(() -> userService.forceWithdraw(adminId, memberId))
                    .isInstanceOf(AlreadyWithdrawnException.class)
                    .hasMessage("이미 탈퇴한 회원입니다.");

            verify(refreshTokenService, never()).revokeAllRefreshTokens(any());
        }
    }
}







