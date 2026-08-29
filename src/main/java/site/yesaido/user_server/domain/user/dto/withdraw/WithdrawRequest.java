package site.yesaido.user_server.domain.user.dto.withdraw;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
