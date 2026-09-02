package site.yesaido.user_server.domain.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import site.yesaido.user_server.domain.user.dto.UserSummaryResponse;
import site.yesaido.user_server.domain.user.dto.profile.PasswordChangeRequest;
import site.yesaido.user_server.domain.user.dto.profile.PasswordVerifyRequest;
import site.yesaido.user_server.domain.user.dto.profile.ProfileUpdateRequest;
import site.yesaido.user_server.domain.user.dto.profile.UserProfileResponse;
import site.yesaido.user_server.domain.user.dto.search.UserSearchResponse;
import site.yesaido.user_server.domain.user.dto.signup.SignupEligibility;
import site.yesaido.user_server.domain.user.dto.signup.SignupEmailVerificationResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignUpRequest;
import site.yesaido.user_server.domain.user.dto.withdraw.WithdrawRequest;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.service.UserService;
import site.yesaido.user_server.global.common.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("닉네임 중복 확인 - 중복이면 true 반환")
    void checkNickname_duplicated() {
        given(userService.existNickname("닉네임")).willReturn(true);

        ResponseEntity<ApiResponse<Boolean>> response = userController.checkNickname("닉네임");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isTrue();
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 중복 아니면 false 반환")
    void checkNickname_notDuplicated() {
        given(userService.existNickname("새닉네임")).willReturn(false);

        ResponseEntity<ApiResponse<Boolean>> response = userController.checkNickname("새닉네임");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isFalse();
    }

    @Test
    @DisplayName("회원가입 성공 시 201과 가입 정보를 반환한다")
    void signUp_success() {
        UserSignUpRequest request = UserSignUpRequest.builder()
                .email("test@test.com")
                .password("password1!")
                .nickName("닉네임")
                .role(Role.USER)
                .build();

        UserSignResponse expected = UserSignResponse.builder()
                .id(1L)
                .email("test@test.com")
                .nickName("닉네임")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        given(userService.signUp(request, null)).willReturn(expected);

        ResponseEntity<ApiResponse<UserSignResponse>> response = userController.signUp(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getEmail()).isEqualTo("test@test.com");
        assertThat(response.getBody().data().getNickName()).isEqualTo("닉네임");
    }

    @Test
    @DisplayName("프로필 사진을 포함한 회원가입 성공 시 201과 가입 정보를 반환한다")
    void signUpWithProfileImage_success() {
        UserSignUpRequest request = UserSignUpRequest.builder()
                .email("test@test.com")
                .password("password1!")
                .nickName("닉네임")
                .role(Role.USER)
                .build();
        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage", "profile.png", "image/png", "image-content".getBytes()
        );
        UserSignResponse expected = UserSignResponse.builder()
                .id(1L)
                .email("test@test.com")
                .nickName("닉네임")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        given(userService.signUp(request, profileImage)).willReturn(expected);

        ResponseEntity<ApiResponse<UserSignResponse>> response = userController.signUp(request, profileImage);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
        verify(userService).signUp(request, profileImage);
    }

    @Test
    @DisplayName("회원가입 이메일 인증 결과를 반환한다")
    void verifySignupEmail_success() {
        SignupEmailVerificationResponse expected = new SignupEmailVerificationResponse(
                true, SignupEligibility.AVAILABLE, null
        );
        given(userService.verifySignupEmail("test@test.com", "123456")).willReturn(expected);

        ResponseEntity<ApiResponse<SignupEmailVerificationResponse>> response =
                userController.verifySignupEmail("test@test.com", "123456");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    @DisplayName("사용자 검색 결과를 그대로 반환한다")
    void search_success() {
        List<UserSearchResponse> expected = List.of(new UserSearchResponse(1L, "닉네임"));
        given(userService.searchUsers("닉")).willReturn(expected);

        ResponseEntity<List<UserSearchResponse>> response = userController.search("닉");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().nickname()).isEqualTo("닉네임");
    }

    @Test
    @DisplayName("ID 목록으로 사용자 요약 정보를 배치 조회한다")
    void getUsers_success() {
        List<UserSummaryResponse> expected = List.of(
                new UserSummaryResponse(1L, "닉네임1"),
                new UserSummaryResponse(2L, "닉네임2")
        );
        given(userService.getUsers(List.of(1L, 2L))).willReturn(expected);

        ResponseEntity<List<UserSummaryResponse>> response = userController.getUsers(List.of(1L, 2L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("마이페이지 프로필 조회 성공")
    void getMyProfile_success() {
        UserProfileResponse expected = mock(UserProfileResponse.class);
        given(userService.getMyProfile(1L)).willReturn(expected);

        ResponseEntity<ApiResponse<UserProfileResponse>> response = userController.getMyProfile(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    @DisplayName("마이페이지 프로필 수정 성공")
    void updateProfile_success() {
        ProfileUpdateRequest request = new ProfileUpdateRequest("새닉네임");
        UserProfileResponse expected = mock(UserProfileResponse.class);
        given(userService.updateProfile(1L, request)).willReturn(expected);

        ResponseEntity<ApiResponse<UserProfileResponse>> response = userController.updateProfile(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(expected);
    }

    @Test
    @DisplayName("비밀번호 변경 요청을 서비스에 위임하고 성공 응답을 반환한다")
    void changePassword_success() {
        PasswordChangeRequest request = new PasswordChangeRequest("currentPass1!", "newPass1!");

        ResponseEntity<ApiResponse<Void>> response = userController.changePassword(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
        verify(userService).changePassword(1L, request);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 성공")
    void uploadProfileImage_success() {
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        given(userService.uploadProfileImage(1L, file)).willReturn("profiles/1/img.jpg");

        ResponseEntity<ApiResponse<String>> response = userController.uploadProfileImage(1L, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo("profiles/1/img.jpg");
    }

    @Test
    @DisplayName("비밀번호 검증 성공")
    void verifyPassword_success() {
        PasswordVerifyRequest request = new PasswordVerifyRequest("password123!");
        given(userService.verifyPassword(1L, "password123!")).willReturn(true);

        ResponseEntity<ApiResponse<Boolean>> response = userController.verifyPassword(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isTrue();
    }

    @Test
    @DisplayName("회원 탈퇴 요청 시 비밀번호를 검증하는 서비스에 위임하고 성공 응답을 반환한다")
    void withdraw_success() {
        WithdrawRequest request = new WithdrawRequest("password123!");

        ResponseEntity<ApiResponse<Void>> response = userController.withdraw(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("회원 탈퇴가 완료되었습니다.");
        verify(userService).withdraw(1L, "password123!");
    }

    @Test
    @DisplayName("Google 전용 계정 탈퇴 요청은 현재 로그인한 사용자의 OAuth 탈퇴 서비스에 위임한다")
    void withdrawOAuth_success() {
        ResponseEntity<ApiResponse<Void>> response = userController.withdrawOAuth(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("탈퇴가 완료되었습니다.");
        verify(userService).withdrawOAuth(1L);
    }
}
