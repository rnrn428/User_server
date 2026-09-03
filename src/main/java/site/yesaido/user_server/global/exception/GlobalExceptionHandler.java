package site.yesaido.user_server.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import site.yesaido.common.exception.client.*;
import site.yesaido.user_server.domain.inquiry.exception.FileDeleteException;
import site.yesaido.user_server.domain.inquiry.exception.FileStorageException;
import site.yesaido.user_server.domain.inquiry.exception.FileUploadException;
import site.yesaido.user_server.domain.user.exception.TooManyRequestException;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 BAD_REQUEST
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleCommonBadRequestException(
            BadRequestException e,
            HttpServletRequest request
    ) {
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 401 UNAUTHORIZED
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.UNAUTHORIZED, e, request);
        return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // 403 FORBIDDEN
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.FORBIDDEN, e, request);
        return buildResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // 404 NOT_FOUND
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.NOT_FOUND, e, request);
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 409 CONFLICT
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException e, HttpServletRequest request){
        logWarnFormat(HttpStatus.CONFLICT, e, request);
        return buildResponse(HttpStatus.CONFLICT, e.getMessage());
    }


    // 400 Bad Request (스프링 내장 요청 파라미터/헤더 누락 오류)
    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleSpringBadRequestException(Exception e, HttpServletRequest request){
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, "잘못된 요청 형식 또는 필수 파라미터/헤더가 누락되었습니다.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        logWarnFormat(HttpStatus.CONTENT_TOO_LARGE, e, request);
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE, "업로드할 수 있는 파일 용량을 초과했습니다.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        logWarnFormat(HttpStatus.METHOD_NOT_ALLOWED, e, request);
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "지원하지 않는 요청 방식입니다."
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaTypeException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        logWarnFormat(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e, request);
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "지원하지 않는 요청 형식입니다."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = resolveValidationMessage(e.getBindingResult());

        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException e,
            HttpServletRequest request
    ) {
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "필수 요청 항목이 누락되었습니다: " + e.getRequestPartName()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다.");
    }


    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequestException(TooManyRequestException e, HttpServletRequest request) {
        logWarnFormat(HttpStatus.TOO_MANY_REQUESTS, e, request);
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
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
            FileDeleteException.class,
            FileStorageException.class
    })
    public ResponseEntity<ErrorResponse> handleFileStorageException(RuntimeException e, HttpServletRequest request) {
        log.error("ERROR [스토리지 처리 실패] API: {} {} | 예외: {} | 상세원인: {}",
                request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,"파일 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
        );
    }


    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e, HttpServletRequest request){
        String message = resolveValidationMessage(e.getBindingResult());
        logWarnFormat(HttpStatus.BAD_REQUEST, e, request);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    private String resolveValidationMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();

        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return "요청 값이 올바르지 않습니다.";
        }

        return fieldError.getDefaultMessage();
    }

    private void logWarnFormat(HttpStatus status, Exception e, HttpServletRequest request){
        log.warn("WARN [요청 예외 발생] API: {} {} | 상태: {} | 예외: {} | 상세원인: {}",
                request.getMethod(), request.getRequestURI(), status.value(), e.getClass().getSimpleName(), e.getMessage());
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
