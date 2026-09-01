package site.yesaido.user_server.domain.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import site.yesaido.user_server.domain.user.dto.MemberSummaryResponse;
import site.yesaido.user_server.domain.user.service.UserService;
import site.yesaido.user_server.global.common.ApiResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    @DisplayName("GET /api/v1/admin/members - 회원 목록 조회 성공")
    void getMembers_success() {
        Pageable pageable = PageRequest.of(0, 8);
        Page<MemberSummaryResponse> page = new PageImpl<>(List.of(mock(MemberSummaryResponse.class)));
        given(userService.getMembers(99L, "active", pageable)).willReturn(page);

        ResponseEntity<ApiResponse<Page<MemberSummaryResponse>>> response =
                adminUserController.getMembers(99L, "active", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("PUT /api/v1/admin/members/{memberId}/dormant-release - 휴면 회원 해제 성공")
    void releaseDormantMember_success() {
        ResponseEntity<ApiResponse<Void>> response =
                adminUserController.releaseDormantMember(99L, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("휴면 계정을 해제했습니다.");
        verify(userService).releaseDormantMember(99L, 1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/members/{memberId} - 강제 탈퇴 성공")
    void forceWithdraw_success() {
        ResponseEntity<ApiResponse<Void>> response =
                adminUserController.forceWithdraw(99L, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("회원을 강제 탈퇴했습니다.");
        verify(userService).forceWithdraw(99L, 1L);
    }
}
