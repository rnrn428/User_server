package site.yesaido.user_server.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.user_server.domain.user.dto.MemberSummaryResponse;
import site.yesaido.user_server.domain.user.service.UserService;
import site.yesaido.user_server.global.common.ApiResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MemberSummaryResponse>>> getMembers(@RequestHeader("X-User-Id") Long userId,
                                                                               @RequestParam(defaultValue = "active") String status,
                                                                               @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MemberSummaryResponse> responses = userService.getMembers(userId, status, pageable);
        ApiResponse<Page<MemberSummaryResponse>> apiResponse = ApiResponse.ok("회원 목록입니다.", responses);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PutMapping("/{memberId}/dormant-release")
    public ResponseEntity<ApiResponse<Void>> releaseDormantMember(@RequestHeader("X-User-Id") Long adminUserId, @PathVariable Long memberId){
        userService.releaseDormantMember(adminUserId, memberId);
        ApiResponse<Void> apiResponse = ApiResponse.ok("휴면 계정을 해제했습니다.", null);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> forceWithdraw(@RequestHeader("X-User-Id") Long adminUserId, @PathVariable Long memberId){
        userService.forceWithdraw(adminUserId, memberId);
        ApiResponse<Void> apiResponse = ApiResponse.ok("회원을 강제 탈퇴했습니다.", null);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
