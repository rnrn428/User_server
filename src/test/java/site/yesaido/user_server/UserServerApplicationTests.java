package site.yesaido.user_server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServerApplicationTests {

    @MockitoBean(name = "googleJwtDecoder")
    private JwtDecoder googleJwtDecoder;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("스프링 컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

}
