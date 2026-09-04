package site.yesaido.user_server.domain.user.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DormantUserScheduler {
    private final UserRepository userRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processDormantUsers(){
        LocalDateTime oneYearAgo = LocalDateTime.now(clock).minusYears(1);
        List<User> dormantCandidates = userRepository.findDormantCandidates(UserStatus.ACTIVE, oneYearAgo);

        if(!dormantCandidates.isEmpty()){
            dormantCandidates.forEach(User::changeToDormant);
            log.info("[휴면 계정 자동 전환] 총 {}명의 계정이 휴면 처리되었습니다.", dormantCandidates.size());
        }
    }
}
