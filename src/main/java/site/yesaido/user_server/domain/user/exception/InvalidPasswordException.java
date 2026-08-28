package site.yesaido.user_server.domain.user.exception;

public class InvalidPasswordException extends RuntimeException {

    private static final String MESSAGE = "비밀번호가 일치하지 않습니다.";

    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException() {
        super(MESSAGE);
    }
}
