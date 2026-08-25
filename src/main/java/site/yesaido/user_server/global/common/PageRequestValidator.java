package site.yesaido.user_server.global.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import site.yesaido.user_server.global.exception.InvalidPageRequestException;

public class PageRequestValidator {

    public static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private PageRequestValidator() {
    }

    public static Pageable of(Integer page, Integer size) {
        if (page == null || page < 0) {
            throw new InvalidPageRequestException("page는 0 이상이어야 합니다.");
        }
        if (size == null || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException("size는 1 이상 " + MAX_PAGE_SIZE + " 이하이어야 합니다.");
        }
        return PageRequest.of(page, size, DEFAULT_SORT);
    }
}