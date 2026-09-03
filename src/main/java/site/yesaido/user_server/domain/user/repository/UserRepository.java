package site.yesaido.user_server.domain.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 로그인할 떄 이메일로 유저 찾기
    Optional<User> findByEmail(String email);
    // 이메일 중복 확인
    boolean existsByEmail(String email);
    // 닉네임 중복 확인
    boolean existsByNickName(String nickname);

    // 재배 멤버 초대용: 닉네임 또는 이메일 부분일치로 활성 사용자 검색
    @Query("SELECT u FROM User u WHERE u.status <> :excludedStatus " +
            "AND (u.nickName LIKE CONCAT('%', :keyword, '%') OR u.email = :keyword)")
    List<User> searchActiveUsers(@Param("keyword") String keyword, @Param("excludedStatus") UserStatus excludedStatus);

    @Query("SELECT u FROM User u WHERE u.status = :status AND (u.lastLoginAt <= :cutoffDate OR (u.lastLoginAt IS NULL AND u.createdAt <= :cutoffDate))")
    List<User> findDormantCandidates(@Param("status") UserStatus status, @Param("cutoffDate")LocalDateTime cutoffDate);

    // 활성화된 유저
    Page<User> findAllByStatusNot(UserStatus status, Pageable pageable);
    // 탈퇴된 유저
    Page<User> findAllByStatus(UserStatus status, Pageable pageable);
}
