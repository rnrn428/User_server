package site.yesaido.user_server.domain.user.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import site.yesaido.user_server.domain.user.dto.MemberSummaryResponse;
import site.yesaido.user_server.global.common.ApiResponse;

/**
 * {@code AdminUserController}의 OpenAPI 문서 정의.
 */
@Tag(name = "관리자 - 회원", description = "회원 목록 조회 · 휴면 해제 · 강제 탈퇴 (관리자 전용)")
public interface AdminUserControllerDocs {

    @Operation(summary = "회원 목록 조회", description = "상태(active/dormant/withdrawn 등)별 회원 목록을 페이지로 반환합니다.")
    ResponseEntity<ApiResponse<Page<MemberSummaryResponse>>> getMembers(
            Long userId,
            @Parameter(description = "회원 상태 필터 (기본값 active)") String status,
            @ParameterObject Pageable pageable);

    @Operation(summary = "휴면 계정 해제", description = "관리자가 특정 회원의 휴면 상태를 해제합니다.")
    ResponseEntity<ApiResponse<Void>> releaseDormantMember(
            Long adminUserId,
            @Parameter(description = "대상 회원 ID") Long memberId);

    @Operation(summary = "회원 강제 탈퇴", description = "관리자가 특정 회원을 강제 탈퇴 처리합니다.")
    ResponseEntity<ApiResponse<Void>> forceWithdraw(
            Long adminUserId,
            @Parameter(description = "대상 회원 ID") Long memberId);
}
