package site.yesaido.user_server.domain.user.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.user.dto.UserSummaryResponse;
import site.yesaido.user_server.domain.user.dto.profile.PasswordChangeRequest;
import site.yesaido.user_server.domain.user.dto.profile.PasswordVerifyRequest;
import site.yesaido.user_server.domain.user.dto.profile.ProfileUpdateRequest;
import site.yesaido.user_server.domain.user.dto.profile.UserProfileResponse;
import site.yesaido.user_server.domain.user.dto.search.UserSearchResponse;
import site.yesaido.user_server.domain.user.dto.signup.SignupEmailVerificationResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignUpRequest;
import site.yesaido.user_server.domain.user.dto.withdraw.WithdrawRequest;
import site.yesaido.user_server.global.common.ApiResponse;

import java.util.List;

/**
 * {@code UserController}의 OpenAPI 문서 정의.
 */
@Tag(name = "사용자", description = "회원가입 · 닉네임 중복 확인 · 마이페이지 · 비밀번호 · 프로필 이미지 · 탈퇴 · 사용자 검색")
public interface UserControllerDocs {

    @Operation(summary = "닉네임 중복 확인", description = "닉네임 사용 가능 여부를 반환합니다. `data=true`면 이미 사용 중. (인증 불필요)")
    ResponseEntity<ApiResponse<Boolean>> checkNickname(@Parameter(description = "확인할 닉네임") String nickName);

    @Operation(summary = "회원가입",
            description = "이메일 인증을 마친 사용자를 등록합니다. `request`(JSON) + `profileImage`(선택) multipart. (인증 불필요)",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가입됨"))
    ResponseEntity<ApiResponse<UserSignResponse>> signUp(UserSignUpRequest request, MultipartFile profileImage);

    @Operation(summary = "회원가입 이메일 인증 확인", description = "회원가입 단계에서 이메일/인증번호를 검증합니다. (인증 불필요)")
    ResponseEntity<ApiResponse<SignupEmailVerificationResponse>> verifySignupEmail(
            @Parameter(description = "이메일") String email,
            @Parameter(description = "인증번호") String code);

    @Operation(summary = "내 프로필 조회", description = "요청자 본인의 프로필을 반환합니다.")
    ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Long userId);

    @Operation(summary = "내 프로필 수정", description = "요청자 본인의 프로필(닉네임 등)을 수정합니다.")
    ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(Long userId, ProfileUpdateRequest request);

    @Operation(summary = "비밀번호 변경", description = "요청자 본인의 비밀번호를 변경합니다. 변경 후 재로그인이 필요합니다.")
    ResponseEntity<ApiResponse<Void>> changePassword(Long userId, PasswordChangeRequest request);

    @Operation(summary = "프로필 이미지 업로드", description = "요청자 본인의 프로필 이미지를 업로드합니다. `data`는 저장된 object key.")
    ResponseEntity<ApiResponse<String>> uploadProfileImage(Long userId, MultipartFile file);

    @Operation(summary = "비밀번호 확인", description = "민감 정보 수정 전 현재 비밀번호가 맞는지 확인합니다.")
    ResponseEntity<ApiResponse<Boolean>> verifyPassword(Long userId, PasswordVerifyRequest request);

    @Operation(summary = "회원 탈퇴", description = "비밀번호 확인 후 요청자 본인 계정을 탈퇴 처리합니다.")
    ResponseEntity<ApiResponse<Void>> withdraw(Long userId, WithdrawRequest request);

    @Operation(summary = "소셜 회원 탈퇴", description = "비밀번호가 없는 소셜(Google) 계정의 탈퇴를 처리합니다.")
    ResponseEntity<ApiResponse<Void>> withdrawOAuth(Long userId);

    @Operation(summary = "사용자 검색",
            description = "재배 멤버 초대 등에 사용. 닉네임 부분일치 또는 이메일 완전일치로 검색합니다.")
    ResponseEntity<List<UserSearchResponse>> search(@Parameter(description = "닉네임/이메일 키워드") String keyword);

    @Operation(summary = "사용자 일괄 조회",
            description = "여러 사용자 ID의 요약 정보를 한 번에 조회합니다. (서비스 간 연동용)")
    ResponseEntity<List<UserSummaryResponse>> getUsers(@Parameter(description = "사용자 ID 목록") List<Long> ids);
}
