package site.yesaido.user_server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * User Server의 OpenAPI(Swagger) 문서 메타데이터를 정의합니다.
 * <p>
 * 엔드포인트별 설명은 각 도메인의 {@code controller.docs} 패키지에 있는 {@code XxxControllerDocs} 인터페이스에 두고,
 * 문서 노출 범위와 Swagger UI 경로는 {@code application.yml} 의 springdoc 설정에서 지정합니다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI userOpenAPI(@Value("${server.port:9002}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("User Server API")
                        .version("v1")
                        .description("""
                                사용자 · 인증 · 문의 서비스 API.

                                회원가입/로그인/토큰 재발급 · 이메일 인증 · 마이페이지 · 소셜(Google) 로그인 ·
                                1:1 문의 및 관리자 답변 · 회원 관리를 제공합니다.

                                ### 인증
                                모든 요청은 API Gateway를 통해 들어오며, Gateway가 JWT를 검증한 뒤
                                `X-User-Id`(필요 시 `X-User-Role`) 헤더를 주입합니다.
                                로그인/회원가입/이메일 인증/토큰 재발급 등 일부 경로는 인증 없이 호출됩니다.

                                ### 응답 형식
                                대부분의 응답은 공통 래퍼 `ApiResponse` 로 감싸집니다:
                                `{ "success": true, "message": "...", "data": ... }`.
                                오류는 RFC 7807 `application/problem+json` 형식으로 반환됩니다.
                                """))
                .servers(List.of(
                        new Server().url("https://api.yes-nhn.site").description("운영 Gateway"),
                        new Server().url("http://localhost:8000").description("로컬 Gateway"),
                        new Server().url("http://localhost:" + serverPort).description("로컬 직접 호출")
                ));
    }
}
