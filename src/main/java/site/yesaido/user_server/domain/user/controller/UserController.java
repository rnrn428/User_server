package site.yesaido.user_server.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.user.controller.docs.UserControllerDocs;
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
import site.yesaido.user_server.domain.user.service.UserService;
import site.yesaido.user_server.global.common.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocs {
    private final UserService userService;

    // 1. 닉네임 중복 체크
    @Override
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestParam("nickname") String nickName){
        boolean isDuplicated = userService.existNickname(nickName);
        ApiResponse<Boolean> apiResponse = ApiResponse.ok("닉네임 중복 체크 결과입니다.", isDuplicated);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 2. 회원가입 (201 Created)
    @Override
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserSignResponse>> signUp(@Valid @RequestPart("request") UserSignUpRequest request, @RequestPart(value = "profileImage", required = false) MultipartFile profileImage){
        UserSignResponse responseDto = userService.signUp(request, profileImage);
        ApiResponse<UserSignResponse> apiResponse = ApiResponse.created("회원가입이 성공적으로 완료되었습니다.", responseDto);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    @PostMapping("/signup/verify-email")
    public ResponseEntity<ApiResponse<SignupEmailVerificationResponse>> verifySignupEmail(
            @RequestParam String email,
            @RequestParam String code
    ) {
        SignupEmailVerificationResponse response = userService.verifySignupEmail(email, code);
        ApiResponse<SignupEmailVerificationResponse> apiResponse = ApiResponse.ok("회원가입 이메일 인증 결과입니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 4. 프로필 조회
    @Override
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(@RequestHeader("X-User-Id") Long userId){
        UserProfileResponse response = userService.getMyProfile(userId);
        ApiResponse<UserProfileResponse> apiResponse = ApiResponse.ok("프로필 조회 성공", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 5. 프로필 수정
    @Override
    @PutMapping("/mypage")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody ProfileUpdateRequest request){
        UserProfileResponse response = userService.updateProfile(userId, request);
        ApiResponse<UserProfileResponse> apiResponse = ApiResponse.ok("프로필 수정 성공", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    @PutMapping("/mypage/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody PasswordChangeRequest request){
        userService.changePassword(userId, request);
        ApiResponse<Void> apiResponse = ApiResponse.ok("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 6. 프로필 이미지
    @Override
    @PutMapping(value = "/mypage/profile-image",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        String objectKey = userService.uploadProfileImage(userId, file);
        ApiResponse<String> apiResponse = ApiResponse.ok("프로필 이미지 업로드 성공", objectKey);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }



    // 7. 프로필 수정 비밀번호 검증
    @Override
    @PostMapping("/verify-password")
    public ResponseEntity<ApiResponse<Boolean>> verifyPassword(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody PasswordVerifyRequest request){
        boolean isValid = userService.verifyPassword(userId, request.password());
        ApiResponse<Boolean> apiResponse = ApiResponse.ok("비밀번호 다시 확인해주세요", isValid);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 8. 회원 탈퇴
    @Override
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody WithdrawRequest request){
        userService.withdraw(userId, request.password());
        ApiResponse<Void> apiResponse = ApiResponse.ok("회원 탈퇴가 완료되었습니다.");
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Override
    @DeleteMapping("/withdraw/oauth")
    public ResponseEntity<ApiResponse<Void>> withdrawOAuth(@RequestHeader("X-User-Id") Long userId){
        userService.withdrawOAuth(userId);
        ApiResponse<Void> apiResponse = ApiResponse.ok("탈퇴가 완료되었습니다.");
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }



    // 재배 멤버 초대용: 닉네임 부분일치 또는 이메일 완전일치로 사용자 검색
    @Override
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> search(@RequestParam("keyword") String keyword){
        List<UserSearchResponse> response = userService.searchUsers(keyword);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/batch")
    public ResponseEntity<List<UserSummaryResponse>> getUsers(@RequestParam("ids") List<Long> ids){
        List<UserSummaryResponse> response = userService.getUsers(ids);
        return ResponseEntity.ok(response);
    }

}
