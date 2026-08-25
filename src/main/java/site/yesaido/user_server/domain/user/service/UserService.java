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
import site.yesaido.user_server.domain.user.dto.MemberSummaryResponse;
import site.yesaido.user_server.domain.user.dto.UserSummaryResponse;
import site.yesaido.user_server.domain.user.dto.profile.ProfileUpdateRequest;
import site.yesaido.user_server.domain.user.dto.profile.UserProfileResponse;
import site.yesaido.user_server.domain.user.dto.search.UserSearchResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignResponse;
import site.yesaido.user_server.domain.user.dto.signup.UserSignUpRequest;
import site.yesaido.user_server.domain.user.entity.ProfileImage;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.*;
import site.yesaido.user_server.domain.user.repository.ProfileImageRepository;
import site.yesaido.user_server.domain.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final ProfileImageRepository profileImageRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserSignResponse signUp(UserSignUpRequest signUpRequestDto){
        if(userRepository.existsByEmail(signUpRequestDto.getEmail())){
            throw new EmailDuplicationException("이메일이 중복됩니다.");
        }

        if(userRepository.existsByNickName(signUpRequestDto.getNickName())){
            throw new NicknameDuplicationException("닉네임이 중복됩니다.");
        }

        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());

        User user = User.builder()
                .email(signUpRequestDto.getEmail())
                .password(encodedPassword)
                .nickName(signUpRequestDto.getNickName())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

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
        return UserProfileResponse.from(user);
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

        if(StringUtils.hasText(request.nickname()) && !user.getNickName().equals(request.nickname())){
            if(userRepository.existsByNickName(request.nickname())){
                throw new NicknameDuplicationException("이미 사용 중인 닉네임입니다.");
            }
            user.updateNickname(request.nickname());
        }

        if(StringUtils.hasText(request.newPassword())){
            if (!StringUtils.hasText(request.currentPassword())) {
                throw new InvalidPasswordException("현재 비밀번호를 입력해 주세요.");
            }
            if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
                throw new InvalidPasswordException("현재 비밀번호가 일치하지 않습니다.");
            }
            user.updatePassword(passwordEncoder.encode(request.newPassword()));
        }
        return UserProfileResponse.from(user);
    }

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile file) {
        User user = getUserById(userId);

        String newObjectKey = minioService.uploadProfileImage(userId, file);

        try{
            String oldObjectKey = replaceProfileImage(user, newObjectKey);

            registerMinioCleanUp(oldObjectKey, newObjectKey);

            return newObjectKey;
        }catch (Exception e){
            minioService.deleteQuietly(newObjectKey);
            throw e;
        }

    }

    @Transactional
    public void withdraw(Long userId) {
        User user = getUserById(userId);
        user.withdraw();
    }

    public boolean existsEmail(String email){
        return userRepository.existsByEmail(email);
    }

    public boolean existNickname(String nickName){
        return userRepository.existsByNickName(nickName);
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

        Page<User> users;
        if ("withdrawn".equalsIgnoreCase(statusFilter)) {
                users = userRepository.findAllByStatus(UserStatus.DELETED, pageable);
        } else if ("active".equalsIgnoreCase(statusFilter)) {
            users = userRepository.findAllByStatusNot(UserStatus.DELETED, pageable);
        } else {
            throw new IllegalArgumentException("지원하지 않는 회원 상태입니다.");
        }
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

}
