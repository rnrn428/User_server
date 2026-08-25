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
import org.springframework.security.crypto.password.PasswordEncoder;
import site.yesaido.user_server.domain.user.dto.MemberSummaryResponse;
import site.yesaido.user_server.domain.user.dto.UserSummaryResponse;
import site.yesaido.user_server.domain.user.dto.profile.ProfileUpdateRequest;
import site.yesaido.user_server.domain.user.dto.profile.UserProfileResponse;
import site.yesaido.user_server.domain.user.dto.search.UserSearchResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignUpRequest;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.*;
import site.yesaido.user_server.domain.user.repository.UserRepository;

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

            given(userRepository.existsByEmail(requestDto.getEmail())).willReturn(false);
            given(userRepository.existsByNickName(requestDto.getNickName())).willReturn(false);
            given(passwordEncoder.encode(requestDto.getPassword())).willReturn("$2a$10$encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            UserSignResponse responseDto = userService.signUp(requestDto);

            assertThat(responseDto).isNotNull();
            assertThat(responseDto.getEmail()).isEqualTo("rnrn428@naver.com");
            assertThat(responseDto.getNickName()).isEqualTo("duplicate");
            verify(userRepository, times(1)).save(any(User.class));
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

            given(userRepository.existsByEmail(requestDto.getEmail())).willReturn(true);

            assertThrows(EmailDuplicationException.class, () -> userService.signUp(requestDto));

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

            given(userRepository.existsByNickName(requestDto.getNickName())).willReturn(true);

            assertThrows(NicknameDuplicationException.class, () -> userService.signUp(requestDto));

            verify(userRepository, never()).save(any(User.class));
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
        }

        @Test
        @DisplayName("닉네임 변경 성공")
        void updateNickname_success(){
            User updateUser = User.builder()
                    .id(1L)
                    .nickName("oldNick")
                    .build();

            ProfileUpdateRequest request = new ProfileUpdateRequest("newNick", null, null);

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

            ProfileUpdateRequest request = new ProfileUpdateRequest("newNick", null, null);


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
        @DisplayName("비밀번호 변경 성공")
        void updatePassword_success() {
            User user = User.builder().id(1L).nickName("nick").password("encodedOld").build();
            ProfileUpdateRequest request = new ProfileUpdateRequest("nick", "rawOld", "rawNew");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("rawOld", "encodedOld")).willReturn(true);
            given(passwordEncoder.encode("rawNew")).willReturn("encodedNew");

            userService.updateProfile(1L, request);

            assertThat(user.getPassword()).isEqualTo("encodedNew");
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 미입력 시 InvalidPasswordException")
        void updatePassword_noCurrentPassword_throwsException() {
            User user = User.builder().id(1L).nickName("nick").password("encodedOld").build();
            ProfileUpdateRequest request = new ProfileUpdateRequest("nick", "", "rawNew");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThrows(site.yesaido.user_server.domain.user.exception.InvalidPasswordException.class,
                    () -> userService.updateProfile(1L, request));
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치 시 InvalidPasswordException")
        void updatePassword_wrongCurrentPassword_throwsException() {
            User user = User.builder().id(1L).nickName("nick").password("encodedOld").build();
            ProfileUpdateRequest request = new ProfileUpdateRequest("nick", "wrongOld", "rawNew");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongOld", "encodedOld")).willReturn(false);

            assertThrows(site.yesaido.user_server.domain.user.exception.InvalidPasswordException.class,
                    () -> userService.updateProfile(1L, request));
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

                String result = userService.uploadProfileImage(1L, file);

                assertThat(result).isEqualTo("profiles/1/new.jpg");
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

                String result = userService.uploadProfileImage(1L, file);

                assertThat(result).isEqualTo("profiles/1/new.jpg");
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
    class withDrawnTest{
        @Test
        @DisplayName("성공 : 회원 탈퇴 시 상태가 DELETED로 바뀌고 deletedAt이 기록된다")
        void success_withdraw(){
            User user = User.builder()
                    .id(1L)
                    .email("rnrn428@naver.com")
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            userService.withdraw(1L);

            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(user.getDeletedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("이메일이 존재하면 true 반환")
    void existsEmail_true(){
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        boolean result = userService.existsEmail("test@test.com");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이메일이 존재하지 않으면 false 반환")
    void existsEmail_false(){
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);

        boolean result = userService.existsEmail("new@test.com");

        assertThat(result).isFalse();
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
        @DisplayName("성공 : status=active면 탈퇴하지 않은 회원을 조회한다")
        void getMembers_active_success() {
            Long adminId = 99L;
            User admin = User.builder().id(adminId).role(Role.ADMIN).build();
            User member = User.builder().id(1L).nickName("닉네임").email("nick@test.com").status(UserStatus.ACTIVE).build();
            Pageable pageable = PageRequest.of(0, 8);
            Page<User> page = new PageImpl<>(List.of(member));

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(userRepository.findAllByStatusNot(UserStatus.DELETED, pageable)).willReturn(page);

            Page<MemberSummaryResponse> result = userService.getMembers(adminId, "active", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().nickname()).isEqualTo("닉네임");
            verify(userRepository, never()).findAllByStatus(any(), any());
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
    }
}




















