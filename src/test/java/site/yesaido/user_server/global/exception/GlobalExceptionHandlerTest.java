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
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import site.yesaido.user_server.domain.user.exception.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;


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
    @DisplayName("NotFoundException 발생 시 404 NOT_FOUND와 에러 메시지를 반환한다")
    void handleNotFoundException_success(){
        setupMockRequest();
        UserNotFoundException e = new UserNotFoundException("존재하지 않는 사용자입니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFoundException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("InvalidPasswordException 발생 시 400 BAD_REQUEST와 에러 메시지를 반환한다")
    void handleInvalidPasswordException_success(){
        setupMockRequest();
        InvalidPasswordException e = new InvalidPasswordException();

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCommonBadRequestException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("EmailDuplicationException 발생 시 409 CONFLICT와 에러 메시지를 반환한다")
    void handleEmailDuplicationException_success(){
        setupMockRequest();
        EmailDuplicationException e = new EmailDuplicationException("이메일이 중복됩니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleConflictException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("이메일이 중복됩니다.");
    }

    @Test
    @DisplayName("NicknameDuplicationException 발생 시 409 CONFLICT와 에러 메시지를 반환한다")
    void handleNicknameDuplicationException_success(){
        setupMockRequest();
        NicknameDuplicationException e = new NicknameDuplicationException("닉네임이 중복됩니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleConflictException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("닉네임이 중복됩니다.");
    }

    @Test
    @DisplayName("AlreadyWithdrawnException 발생 시 400 BAD_REQUEST와 에러 메시지를 반환한다")
    void handleAlreadyWithdrawnException_success(){
        setupMockRequest();
        AlreadyWithdrawnException e = new AlreadyWithdrawnException("이미 탈퇴한 회원입니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCommonBadRequestException(e, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("이미 탈퇴한 회원입니다.");
    }

    @Test
    @DisplayName("InvalidTokenException 발생 시 401 UNAUTHORIZED와 에러 메시지를 반환한다")
    void handleInvalidTokenException_success(){
        setupMockRequest();
        InvalidTokenException e = new InvalidTokenException("유효하지 않은 토큰입니다.");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedException(e, request);

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

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleForbiddenException(e, request);

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
        assertThat(response.getBody().getMessage())
                .isEqualTo("파일 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @Test
    @DisplayName("검증 오류 발생 시 첫 번째 필드 오류 메시지로 400 BAD_REQUEST를 반환한다")
    void handleMethodArgumentNotValidException_success() {
        setupMockRequest();
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        given(exception.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldError()).willReturn(new FieldError("request", "email", "이메일 형식이 올바르지 않습니다."));

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("이메일 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("필수 multipart 항목이 누락되면 400 BAD_REQUEST를 반환한다")
    void handleMissingServletRequestPartException_success() {
        setupMockRequest();
        MissingServletRequestPartException exception = new MissingServletRequestPartException("profileImage");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMissingServletRequestPartException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("필수 요청 항목이 누락되었습니다: profileImage");
    }

    @Test
    @DisplayName("BindException에 필드 오류가 없으면 기본 메시지로 400 BAD_REQUEST를 반환한다")
    void handleBindExceptionWithoutFieldError_returnsFallbackMessage() {
        setupMockRequest();
        BindException exception = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        given(exception.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldError()).willReturn(null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBindException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("요청 값이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드면 405 METHOD_NOT_ALLOWED를 반환한다")
    void handleMethodNotAllowedException_success() {
        setupMockRequest();
        HttpRequestMethodNotSupportedException exception = mock(HttpRequestMethodNotSupportedException.class);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodNotAllowedException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("지원하지 않는 요청 방식입니다.");
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type이면 415 UNSUPPORTED_MEDIA_TYPE을 반환한다")
    void handleUnsupportedMediaTypeException_success() {
        setupMockRequest();
        HttpMediaTypeNotSupportedException exception = mock(HttpMediaTypeNotSupportedException.class);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnsupportedMediaTypeException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("지원하지 않는 요청 형식입니다.");
    }

    @Test
    @DisplayName("업로드 파일 용량을 초과하면 413 CONTENT_TOO_LARGE를 반환한다")
    void handleMaxUploadSizeExceededException_success() {
        setupMockRequest();
        MaxUploadSizeExceededException exception = mock(MaxUploadSizeExceededException.class);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleMaxUploadSizeExceededException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("업로드할 수 있는 파일 용량을 초과했습니다.");
    }
}
