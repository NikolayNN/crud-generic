package by.nhorushko.filterspecification;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PageRequestBuilder {
    private PageRequestBuilder() {
        // Do nothing
    }

    /**
     * Constructs PageRequest
     *
     * 'sort' query param with comma separated attributes prefixed with either '+' (ASC Order) or '-' (DESC Order) are
     * converted to org.springframework.data.domain.Sort with org.springframework.data.domain.PageRequest
     *
     * Example: .....?sort=+salary,+joiningDate
     *
     * @param pageSize
     * @param pageNumber
     * @param sortingCriteria
     * @return
     */
    public static PageRequest getPageRequest(Integer pageSize, Integer pageNumber, String sortingCriteria) {

        Set<String> sortingFileds = new LinkedHashSet<>(
                Arrays.asList(StringUtils.split(StringUtils.defaultIfEmpty(sortingCriteria, ""), ",")));

        List<Sort.Order> sortingOrders = sortingFileds.stream().map(PageRequestBuilder::getOrder)
                .collect(Collectors.toList());

        Sort sort = sortingOrders.isEmpty() ? null : Sort.by(sortingOrders);

        return PageRequest.of(ObjectUtils.defaultIfNull(pageNumber, 0), ObjectUtils.defaultIfNull(pageSize, 20),
                sort);
    }

    private static Sort.Order getOrder(String value) {

        if (StringUtils.startsWith(value, "-")) {
            return new Sort.Order(Sort.Direction.DESC, StringUtils.substringAfter(value, "-"));
        } else if (StringUtils.startsWith(value, "+")) {
            return new Sort.Order(Sort.Direction.ASC, StringUtils.substringAfter(value, "+"));
        } else {
            // Sometimes '+' from query param can be replaced as ' '
            return new Sort.Order(Sort.Direction.ASC, StringUtils.trim(value));
        }

    }
}
