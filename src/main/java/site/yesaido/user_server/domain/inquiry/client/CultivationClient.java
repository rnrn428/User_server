package site.yesaido.user_server.domain.inquiry.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import site.yesaido.user_server.domain.inquiry.dto.response.CultivationSummaryResponse;

@FeignClient(name = "cultivation-server", url = "${feign.client.cultivation.url}")
public interface CultivationClient {
    @GetMapping("/api/v1/cultivations/{cultivation-id}")
    CultivationSummaryResponse getCultivation(@RequestHeader("X-User-Id") Long userId,
                                              @PathVariable("cultivation-id") Long cultivationId);
}
