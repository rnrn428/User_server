package site.yesaido.user_server.domain.user.dto.signup;

import java.time.LocalDateTime;

public record SignupEmailVerificationResponse(
        boolean verified,
        SignupEligibility eligibility,
        LocalDateTime rejoinAvailableAt
) {
    public static SignupEmailVerificationResponse notVerified() {
        return new SignupEmailVerificationResponse(false, null, null);
    }

    public static SignupEmailVerificationResponse available() {
        return new SignupEmailVerificationResponse(true, SignupEligibility.AVAILABLE, null);
    }

    public static SignupEmailVerificationResponse alreadyRegistered() {
        return new SignupEmailVerificationResponse(true, SignupEligibility.ALREADY_REGISTERED, null);
    }

    public static SignupEmailVerificationResponse rejoinRestricted(LocalDateTime rejoinAvailableAt) {
        return new SignupEmailVerificationResponse(true, SignupEligibility.REJOIN_RESTRICTED, rejoinAvailableAt);
    }
}
