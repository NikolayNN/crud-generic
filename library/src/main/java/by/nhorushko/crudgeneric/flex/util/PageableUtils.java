package by.nhorushko.crudgeneric.flex.util;

import by.nhorushko.filterspecification.PageRequestBuilder;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public class PageableUtils {

    public static Pageable buildPageRequest(int page, int size, String sort) {
        sort = sortAliases(sort);
        return PageRequestBuilder.getPageRequest(size, page, sort);
    }

    public static Pageable buildPageRequest(int page, int size, String sort, Map<String, String> entityFieldPaths) {
        sort = sortAliases(sort);
        for (String k : entityFieldPaths.keySet()) {
            sort.replaceAll(k, entityFieldPaths.get(k));
        }
        return buildPageRequest(page, size, sort);
    }

    private static String sortAliases(String sort) {
        sort = sort.replace("asc#", "+");
        return sort.replace("desc#", "-");
    }
}
