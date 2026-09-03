package site.yesaido.user_server.domain.user.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


// searchActiveUsers()의 실제 @Query 동작(닉네임 부분일치·이메일 완전일치·탈퇴자 제외)을
// 검증하기 위한 리포지토리 레벨 통합 테스트. UserServiceTest는 mock 기반이라 이 부분을
// 실제로 검증하지 못했다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("닉네임에 검색어가 포함된 활성 사용자를 부분일치로 찾는다")
    void searchActiveUsers_nicknamePartialMatch() {
        userRepository.save(activeUser("member@example.com", "진영이짱"));
        userRepository.save(activeUser("other@example.com", "관계없는닉네임"));

        List<User> result = userRepository.searchActiveUsers("진영", UserStatus.DELETED);

        assertThat(result).extracting(User::getNickName).containsExactly("진영이짱");
    }

    @Test
    @DisplayName("이메일은 완전일치일 때만 검색된다")
    void searchActiveUsers_emailExactMatchOnly() {
        userRepository.save(activeUser("exact@example.com", "닉네임A"));

        List<User> exactMatch = userRepository.searchActiveUsers("exact@example.com", UserStatus.DELETED);
        List<User> partialMatch = userRepository.searchActiveUsers("exact@example", UserStatus.DELETED);

        assertThat(exactMatch).extracting(User::getEmail).containsExactly("exact@example.com");
        assertThat(partialMatch).isEmpty();
    }

    @Test
    @DisplayName("탈퇴(DELETED) 상태의 사용자는 검색 결과에서 제외된다")
    void searchActiveUsers_excludesDeletedUsers() {
        User deletedUser = activeUser("deleted@example.com", "탈퇴자닉네임");
        deletedUser.withdraw();
        userRepository.save(deletedUser);

        List<User> result = userRepository.searchActiveUsers("탈퇴자", UserStatus.DELETED);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("일치하는 사용자가 없으면 빈 리스트를 반환한다")
    void searchActiveUsers_noMatch() {
        userRepository.save(activeUser("someone@example.com", "아무개"));

        List<User> result = userRepository.searchActiveUsers("존재하지않는키워드", UserStatus.DELETED);

        assertThat(result).isEmpty();
    }

    private User activeUser(String email, String nickname) {
        return User.builder()
                .email(email)
                .nickName(nickname)
                .status(UserStatus.ACTIVE)
                .build();
    }
}