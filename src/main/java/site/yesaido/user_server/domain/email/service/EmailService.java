package site.yesaido.user_server.domain.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import site.yesaido.user_server.domain.user.exception.TooManyRequestException;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final AsyncMailSender asyncMailSender;
    private final StringRedisTemplate stringRedisTemplate;
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final String CODE_PREFIX = "EMAIL_VERIFY:"; // 이메일당 6자리 인증번호 저장용 (수명 : 3분)
    private static final String RESEND_WAIT_PREFIX = "EMAIL_RESEND_WAIT:"; // 연속 클릭 방지용 (수명 : 30초)
    private static final String VERIFY_FAIL_PREFIX = "EMAIL_VERIFY_FAIL:"; // 인증번호 틀린 횟수 카운트용 (수명 : 5분)
    private static final String SIGNUP_VERIFIED_PREFIX = "EMAIL_SIGNUP_VERIFIED:";

    private static final long RESEND_WAIT_SECONDS = 30;
    private static final long MAX_VERIFY_FAIL_COUNT = 5;
    private static final long VERIFY_FAIL_WINDOW_MINUTES = 5;


    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail){
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(getCooldownKey(toEmail)))) {
            throw new TooManyRequestException("잠시 후 다시 시도해주세요. (30초)");
        }

        String authCode = String.format("%06d", secureRandom.nextInt(1000000));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[MushMush] 이메일 본인 인증번호입니다.");
        message.setText("안녕하세요! MushMush 인증번호는 [" + authCode + "] 입니다.\n3분 이내에 입력해 주세요.");

        asyncMailSender.sendMailAsync(message);

        stringRedisTemplate.opsForValue().set(getCodeKey(toEmail), authCode, 3, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(getCooldownKey(toEmail), "1", RESEND_WAIT_SECONDS, TimeUnit.SECONDS);
        stringRedisTemplate.delete(getFailCountKey(toEmail));
    }

    public boolean verifyCode(String email, String inputCode){
        String failCountStr = stringRedisTemplate.opsForValue().get(getFailCountKey(email));
        long failCount = failCountStr == null ? 0 : Long.parseLong(failCountStr);
        if (failCount >= MAX_VERIFY_FAIL_COUNT) {
            throw new TooManyRequestException("인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }

        String savedCode = stringRedisTemplate.opsForValue().get(getCodeKey(email));

        if(savedCode == null || !savedCode.equals(inputCode)){
            Long newFailCount = stringRedisTemplate.opsForValue().increment(getFailCountKey(email));
            if (newFailCount != null && newFailCount == 1L) {
                stringRedisTemplate.expire(getFailCountKey(email), VERIFY_FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
            }
            return false;
        }

        stringRedisTemplate.delete(getCodeKey(email));
        stringRedisTemplate.delete(getFailCountKey(email));
        return true;
    }

    public boolean verifySignupCode(String email, String inputCode) {
        boolean verified = verifyCode(email, inputCode);
        if (verified) {
            stringRedisTemplate.opsForValue().set(getSignupVerifiedKey(email), "1", 5, TimeUnit.MINUTES);
        }
        return verified;
    }

    public boolean isSignupEmailVerified(String email) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(getSignupVerifiedKey(email)));
    }

    public void clearSignupEmailVerification(String email) {
        stringRedisTemplate.delete(getSignupVerifiedKey(email));
    }

    private String getCodeKey(String email){
        return CODE_PREFIX+ email;
    }

    private String getCooldownKey(String email){
        return RESEND_WAIT_PREFIX + email;
    }

    private String getFailCountKey(String email){
        return VERIFY_FAIL_PREFIX + email;
    }

    private String getSignupVerifiedKey(String email) {
        return SIGNUP_VERIFIED_PREFIX + email;
    }

}
