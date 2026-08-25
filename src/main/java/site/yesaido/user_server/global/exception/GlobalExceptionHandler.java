package site.yesaido.user_server.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import site.yesaido.user_server.domain.inquiry.exception.*;
import site.yesaido.user_server.domain.user.exception.*;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 404
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.NOT_FOUND, e, request);
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({
            InquiryNotFoundException.class,
            InquiryCategoryNotFoundException.class,
            InquiryAnswerNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleInquiryNotFoundException(Exception e, HttpServletRequest request){
        logWarnFormat(HttpStatus.NOT_FOUND, e, request);
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 400
    @ExceptionHandler({
            NicknameDuplicationException.class,
            EmailDuplicationException.class,
            InvalidPasswordException.class,
            AlreadyWithdrawnException.class,
            DormantUserException.class,
            InvalidPageRequestException.class,
            InquiryAnswerThreadMismatchException.class,
            InvalidFileException.class,
            InquiryPhotoLimitExceededException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(RuntimeException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 400 Bad Request (스프링 내장 요청 파라미터/헤더 누락 오류)
    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MaxUploadSizeExceededException.class
    })
    public ResponseEntity<ErrorResponse> handleSpringBadRequestException(Exception e, HttpServletRequest request){
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, "잘못된 요청 형식 또는 필수 파라미터/헤더가 누락되었습니다.");
    }

    // 401
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.UNAUTHORIZED, e, request);
        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequestException(TooManyRequestException e, HttpServletRequest request) {
        logWarnFormat(HttpStatus.TOO_MANY_REQUESTS, e, request);
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    // 403
    @ExceptionHandler(InquiryAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleInquiryAccessDeniedException(InquiryAccessDeniedException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.FORBIDDEN, e, request);
        return buildResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(UserAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleUserAccessDeniedException(UserAccessDeniedException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.FORBIDDEN, e, request);
        return buildResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e, HttpServletRequest request){
        log.error("ERROR [서버 장애 발생] API: {} {} | 예외: {} | 상세원인: {}",
                request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");
    }

    // 500 (MinIO 스토리지 파일 업로드/삭제 실패)
    @ExceptionHandler({
            FileUploadException.class,
            FileDeleteException.class
    })
    public ResponseEntity<ErrorResponse> handleFileStorageException(RuntimeException e, HttpServletRequest request) {
        log.error("ERROR [스토리지 처리 실패] API: {} {} | 예외: {} | 상세원인: {}",
                request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }


    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e, HttpServletRequest request){
        String errorMessage = e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private void logWarnFormat(HttpStatus status, Exception e, HttpServletRequest request){
        log.warn("WARN [요청 예외 발생] API: {} {} | 상태: {} | 예외: {} | 상세원인: {}",
                request.getMethod(), request.getRequestURI(), status.value(), e.getClass().getSimpleName(), e.getMessage(), e);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message){
        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .error(status.name())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(response);
    }

}
