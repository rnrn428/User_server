package site.yesaido.user_server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import site.yesaido.user_server.domain.email.service.EmailService;
import site.yesaido.user_server.domain.user.dto.MemberSummaryResponse;
import site.yesaido.user_server.domain.user.dto.UserSummaryResponse;
import site.yesaido.user_server.domain.user.dto.profile.PasswordChangeRequest;
import site.yesaido.user_server.domain.user.dto.profile.ProfileUpdateRequest;
import site.yesaido.user_server.domain.user.dto.profile.UserProfileResponse;
import site.yesaido.user_server.domain.user.dto.search.UserSearchResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignUpRequest;
import site.yesaido.user_server.domain.user.dto.signup.SignupEmailVerificationResponse;
import site.yesaido.user_server.domain.user.entity.ProfileImage;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.*;
import site.yesaido.user_server.domain.user.repository.ProfileImageRepository;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenService;

import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final long REJOIN_RESTRICTION_DAYS = 30;

    private final UserRepository userRepository;
    private final MinioService minioService;
    private final ProfileImageRepository profileImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Transactional
    public UserSignResponse signUp(UserSignUpRequest signUpRequestDto, MultipartFile profileImage){
        String email = signUpRequestDto.getEmail().trim();
        validateSignupEmailVerification(email);

        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null){
            if(existingUser.getStatus() != UserStatus.DELETED){
                throw new EmailDuplicationException("이메일이 중복됩니다.");
            }
            validateRejoinAllowed(existingUser);
            existingUser.anonymize();
        }


        if(userRepository.existsByNickName(signUpRequestDto.getNickName())){
            throw new NicknameDuplicationException("닉네임이 중복됩니다.");
        }


        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(signUpRequestDto.getPassword()))
                .nickName(signUpRequestDto.getNickName())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        if(profileImage != null && !profileImage.isEmpty()){
            saveInitialProfileImage(savedUser, profileImage);
        }
        emailService.clearSignupEmailVerification(email);

        return UserSignResponse.from(savedUser);
    }


    public User getUserById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("아이디를 찾을 수 없습니다."));

        if(UserStatus.DELETED.equals(user.getStatus())){
            throw new AlreadyWithdrawnException("이미 탈퇴한 사용자입니다.");
        }

        return user;
    }

    public UserProfileResponse getMyProfile(Long userId){
        User user = getUserById(userId);
        return UserProfileResponse.from(user, resolveProfilePhotoUrl(userId));
    }

    public boolean verifyPassword(Long userId, String rawPassword){
        if(userId == null){
            return false;
        }
        User user = getUserById(userId);
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, ProfileUpdateRequest request){
        User user = getUserById(userId);

        if(!user.getNickName().equals(request.nickname())){
            if(userRepository.existsByNickName(request.nickname())){
                throw new NicknameDuplicationException("이미 사용 중인 닉네임입니다.");
            }
            user.updateNickname(request.nickname());
        }
        return UserProfileResponse.from(user, resolveProfilePhotoUrl(userId));
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request){
        User user = getUserById(userId);

        if(user.getPassword() == null){
            throw new InvalidPasswordException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
            throw new InvalidPasswordException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())){
            throw new InvalidPasswordException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllRefreshTokens(userId);
    }

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile file) {
        User user = getUserById(userId);

        String newObjectKey = minioService.uploadProfileImage(userId, file);

        try{
            String oldObjectKey = replaceProfileImage(user, newObjectKey);

            registerMinioCleanUp(oldObjectKey, newObjectKey);

            return minioService.presignedGetUrl(newObjectKey);
        }catch (Exception e){
            minioService.deleteQuietly(newObjectKey);
            throw e;
        }
    }

    @Transactional
    public void withdraw(Long userId, String password) {
        User user = getUserById(userId);
        if(user.getPassword() == null){
            throw new InvalidPasswordException("소셜 로그인 계정은 비밀번호로 탈퇴할 수 없습니다.");
        }

        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }

        user.withdraw();
        refreshTokenService.revokeAllRefreshTokens(userId);
    }

    public SignupEmailVerificationResponse verifySignupEmail(String email, String code) {
        String normalizedEmail = email.trim();
        if (!emailService.verifySignupCode(normalizedEmail, code.trim())) {
            return SignupEmailVerificationResponse.notVerified();
        }

        return userRepository.findByEmail(normalizedEmail)
                .map(this::getSignupEligibility)
                .orElseGet(SignupEmailVerificationResponse::available);
    }

    public boolean existNickname(String nickName){
        return userRepository.existsByNickName(nickName);
    }

    @Transactional
    public void releaseDormantMember(Long adminUserId, Long memberId){
        requireAdmin(adminUserId);
        User member = userRepository.findById(memberId).orElseThrow(UserNotFoundException::new);

        if(member.getStatus() != UserStatus.DORMANT){
            throw new IllegalArgumentException("휴면 상태의 회원만 해제할 수 있습니다.");
        }

        member.releaseDormant();
    }

    @Transactional
    public void forceWithdraw(Long adminUserId, Long memberId){
        requireAdmin(adminUserId);

        User member = userRepository.findById(memberId).orElseThrow(UserNotFoundException::new);

        if(member.getRole() == Role.ADMIN){
            throw new IllegalArgumentException("관리자 계정은 강제 탈퇴할 수 없습니다.");
        }

        if(member.getStatus() == UserStatus.DELETED){
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        member.withdraw();
        refreshTokenService.revokeAllRefreshTokens(memberId);
    }
    // 재배 멤버 초대용: 닉네임 부분일치 또는 이메일 완전일치로 활성 사용자 검색
    public List<UserSearchResponse> searchUsers(String keyword){
        if(!StringUtils.hasText(keyword)){
            return Collections.emptyList();
        }

        return userRepository.searchActiveUsers(keyword.trim(), UserStatus.DELETED).stream()
                .map(UserSearchResponse::from)
                .toList();
    }

    public List<UserSummaryResponse> getUsers(List<Long> userIds){
        return userRepository.findAllById(userIds).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    // 관리자 페이지용 유저 조회
    public Page<MemberSummaryResponse> getMembers(Long adminUserId, String statusFilter, Pageable pageable) {
        requireAdmin(adminUserId);

        Page<User> users = switch (statusFilter.toLowerCase()){
            case "active" -> userRepository.findAllByStatus(UserStatus.ACTIVE, pageable);
            case "dormant" -> userRepository.findAllByStatus(UserStatus.DORMANT, pageable);
            case "withdrawn" -> userRepository.findAllByStatus(UserStatus.DELETED, pageable);
            default -> throw new IllegalArgumentException("지원하지 않는 회원 상태입니다.");
        };

        return users.map(MemberSummaryResponse::from);
    }

    private String replaceProfileImage(User user, String newObjectKey){
        return profileImageRepository.findByUserId(user.getId())
                .map(profileImage -> {
                    String oldObjectKey = profileImage.getObjectKey();
                    profileImage.updateObjectKey(newObjectKey);
                    return oldObjectKey;
                })
                .orElseGet(()->{
                    ProfileImage profileImage = ProfileImage.create(user, newObjectKey);
                    profileImageRepository.save(profileImage);
                    return null;
                });
    }

    private void registerMinioCleanUp(String oldObjectKey, String newObjectKey){
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() { // 커밋 확정 후 옛날 사진 안전 삭제
                        minioService.deleteQuietly(oldObjectKey);
                    }

                    @Override
                    public void afterCompletion(int status) { // 롤백/실패 시 새 사진 삭제
                        if(status != TransactionSynchronization.STATUS_COMMITTED){
                            minioService.deleteQuietly(newObjectKey);
                        }
                    }
                }
        );
    }

    private void requireAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != Role.ADMIN) {
            throw new UserAccessDeniedException();
        }
    }

    private SignupEmailVerificationResponse getSignupEligibility(User user) {
        if (user.getStatus() != UserStatus.DELETED) {
            return SignupEmailVerificationResponse.alreadyRegistered();
        }

        LocalDateTime rejoinAvailableAt = getRejoinAvailableAt(user);
        if (LocalDateTime.now(KOREA_ZONE).isBefore(rejoinAvailableAt)) {
            return SignupEmailVerificationResponse.rejoinRestricted(rejoinAvailableAt);
        }
        return SignupEmailVerificationResponse.available();
    }

    private void validateSignupEmailVerification(String email) {
        if (!emailService.isSignupEmailVerified(email)) {
            throw new EmailVerificationRequiredException();
        }
    }

    private void validateRejoinAllowed(User user) {
        LocalDateTime rejoinAvailableAt = getRejoinAvailableAt(user);
        if (LocalDateTime.now(KOREA_ZONE).isBefore(rejoinAvailableAt)) {
            throw new RejoinRestrictedException(rejoinAvailableAt);
        }
    }

    private LocalDateTime getRejoinAvailableAt(User user) {
        if (user.getDeletedAt() == null) {
            throw new IllegalStateException("탈퇴 일시가 없는 탈퇴 회원입니다.");
        }
        return user.getDeletedAt().plusDays(REJOIN_RESTRICTION_DAYS);
    }

    private String resolveProfilePhotoUrl(Long userId) {
        return profileImageRepository.findByUserId(userId)
                .map(ProfileImage::getObjectKey)
                .map(minioService::presignedGetUrl)
                .orElse(null);
    }

    private void saveInitialProfileImage(User user, MultipartFile profileImage){
        String objectKey = minioService.uploadProfileImage(user.getId(), profileImage);
        try{
            ProfileImage image = ProfileImage.create(user, objectKey);
            profileImageRepository.save(image);

            registerMinioCleanUp(null, objectKey);
        }catch (Exception e){
            minioService.deleteQuietly(objectKey);
            throw e;
        }
    }
}
