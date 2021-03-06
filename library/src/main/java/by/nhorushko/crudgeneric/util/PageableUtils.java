package by.nhorushko.crudgeneric.util;

import by.nhorushko.filterspecification.PageRequestBuilder;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public class PageableUtils {

    public static Pageable buildPageRequest(int page, int size, String sort) {
        return PageRequestBuilder.getPageRequest(size, page, sort);
    }

    public static Pageable buildPageRequest(int page, int size, String sort, Map<String, String> entityFieldPaths) {
        for (String k : entityFieldPaths.keySet()) {
            sort.replaceAll(k, entityFieldPaths.get(k));
        }
        return buildPageRequest(page, size, sort);
    }
}
