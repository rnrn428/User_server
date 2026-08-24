package site.yesaido.user_server.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import site.yesaido.user_server.domain.user.exception.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private HttpServletRequest request;

    private void setupMockRequest(){
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/v1/auth/login");
    }

    @Test
    @DisplayName("UserNotFoundException 발생 시 404 NOT_FOUND와 에러 메시지를 반환한다")
    void handleUserNotFoundException_success(){
        setupMockRequest();
        UserNotFoundException e = new UserNotFoundException("존재하지 않는 사용자입니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUserNotFoundException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("InvalidPasswordException 발생 시 400 BAD_REQUEST와 에러 메시지를 반환한다")
    void handleInvalidPasswordException_success(){
        setupMockRequest();
        InvalidPasswordException e = new InvalidPasswordException();

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequestException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("EmailDuplicationException 발생 시 400 BAD_REQUEST와 에러 메시지를 반환한다")
    void handleEmailDuplicationException_success(){
        setupMockRequest();
        EmailDuplicationException e = new EmailDuplicationException("이메일이 중복됩니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequestException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("이메일이 중복됩니다.");
    }

    @Test
    @DisplayName("NicknameDuplicationException 발생 시 400 BAD_REQUEST와 에러 메시지를 반환한다")
    void handleNicknameDuplicationException_success(){
        setupMockRequest();
        NicknameDuplicationException e = new NicknameDuplicationException("닉네임이 중복됩니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequestException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("닉네임이 중복됩니다.");
    }

    @Test
    @DisplayName("AlreadyWithdrawnException 발생 시 400 BAD_REQUEST와 에러 메시지를 반환한다")
    void handleAlreadyWithdrawnException_success(){
        setupMockRequest();
        AlreadyWithdrawnException e = new AlreadyWithdrawnException("이미 탈퇴한 회원입니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBadRequestException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("이미 탈퇴한 회원입니다.");
    }

    @Test
    @DisplayName("InvalidTokenException 발생 시 400 BAD_REQUEST와 에러 메시지를 반환한다")
    void handleInvalidTokenException_success(){
        setupMockRequest();
        InvalidTokenException e = new InvalidTokenException("유효하지 않은 토큰입니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInvalidTokenException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("일반 Exception 발생 시 500 INTERNAL_SERVER_ERROR와 서버 내부 오류 메시지를 반환한다")
    void handleGeneralException_success() {
        setupMockRequest();
        Exception e = new RuntimeException("예측 실패 에러");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneralException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류가 발생했습니다");
    }

    @Test
    @DisplayName("InquiryAccessDeniedException 발생 시 403 FORBIDDEN을 반환한다")
    void handleInquiryAccessDeniedException_success() {
        setupMockRequest();
        site.yesaido.user_server.domain.inquiry.exception.InquiryAccessDeniedException e =
                new site.yesaido.user_server.domain.inquiry.exception.InquiryAccessDeniedException("접근 권한이 없습니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInquiryAccessDeniedException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("TooManyRequestException 발생 시 429 TOO_MANY_REQUESTS를 반환한다")
    void handleTooManyRequestException_success() {
        setupMockRequest();
        site.yesaido.user_server.domain.user.exception.TooManyRequestException e =
                new site.yesaido.user_server.domain.user.exception.TooManyRequestException("요청이 너무 많습니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTooManyRequestException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("요청이 너무 많습니다.");
    }

    @Test
    @DisplayName("FileDeleteException 발생 시 500 INTERNAL_SERVER_ERROR와 메시지를 반환한다")
    void handleFileStorageException_success() {
        setupMockRequest();
        site.yesaido.user_server.domain.inquiry.exception.FileDeleteException e =
                new site.yesaido.user_server.domain.inquiry.exception.FileDeleteException("사진 삭제 실패");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleFileStorageException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("사진 삭제 실패");
    }
}
