package by.nhorushko.crudgeneric.v2.pageable;

import by.nhorushko.crudgeneric.v2.controller.BasePageRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class PageFilterRequest {

    private int page;
    private int pageSize;
    /**
     * sort look like sign + OR - and property name
     * +id
     * -id
     * +name
     * -name
     */
    private String sort;
    private FilterGroup filterGroup;

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getSort() {
        return sort;
    }

    public FilterGroup getFilterGroup() {
        return filterGroup;
    }

    public PageFilterRequest(int page, int pageSize, String sort, FilterGroup filterGroup) {
        this.page = page;
        this.pageSize = pageSize;
        this.sort = sort;
        this.filterGroup = filterGroup;
    }

    private PageFilterRequest(int page, int pageSize, String sort, ConcatCondition condition, Filter... filterGroup) {
        this.page = page;
        this.pageSize = pageSize;
        this.sort = sort;
        this.filterGroup = new FilterGroup(
                Arrays.stream(filterGroup)
                        .filter(f -> StringUtils.isNotEmpty(f.getFilter()))
                        .collect(Collectors.toList()),
                condition);
    }

    public static PageFilterRequest pageRequest(BasePageRequest pageRequest, FilterGroup group) {
        return pageRequest(pageRequest.getPage(), pageRequest.getSize(), pageRequest.getSort(), group);
    }

    public static PageFilterRequest pageRequest(int page, int pageSize, String sort, FilterGroup group) {
        return new PageFilterRequest(page, pageSize, sort, group);
    }

    public static PageFilterRequest pageRequestOr(BasePageRequest pageRequest, Filter... filters) {
        return pageRequestOr(pageRequest.getPage(), pageRequest.getSize(), pageRequest.getSort(), filters);
    }

    public static PageFilterRequest pageRequestOr(int page, int pageSize, String sort, Filter... filters) {
        return new PageFilterRequest(page, pageSize, sort, ConcatCondition.OR, filters);
    }

    public static PageFilterRequest pageRequestAnd(BasePageRequest pageRequest, Filter... filters) {
        return pageRequestAnd(pageRequest.getPage(), pageRequest.getSize(), pageRequest.getSort(), filters);
    }

    public static PageFilterRequest pageRequestAnd(int page, int pageSize, String sort, Filter... filters) {
        return new PageFilterRequest(page, pageSize, sort, ConcatCondition.AND, filters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageFilterRequest that = (PageFilterRequest) o;
        return page == that.page && pageSize == that.pageSize && sort.equals(that.sort) && filterGroup.equals(that.filterGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, pageSize, sort, filterGroup);
    }

    @Override
    public String toString() {
        return "PageRequest{" +
                "page=" + page +
                ", pageSize=" + pageSize +
                ", sort='" + sort + '\'' +
                ", filters=" + filterGroup +
                '}';
    }

    public static class FilterGroup {
        private final List<Filter> filters;
        private final ConcatCondition condition;
        private final List<FilterGroup> subGroups;

        public FilterGroup(List<Filter> filters, ConcatCondition condition) {
            this.filters = filters;
            this.condition = condition;
            this.subGroups = new LinkedList<>();
        }

        public FilterGroup(List<Filter> filters, ConcatCondition condition, List<FilterGroup> subGroups) {
            this.filters = filters;
            this.condition = condition;
            this.subGroups = subGroups;
        }


        public List<Filter> getFilters() {
            return filters;
        }

        public ConcatCondition getCondition() {
            return condition;
        }

        public List<FilterGroup> getSubGroups() {
            return subGroups;
        }

        public boolean isEmpty() {
            return CollectionUtils.isEmpty(filters) && CollectionUtils.isEmpty(subGroups);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterGroup filters1 = (FilterGroup) o;
            return Objects.equals(filters, filters1.filters) && condition == filters1.condition;
        }

        @Override
        public int hashCode() {
            return Objects.hash(filters, condition);
        }

        @Override
        public String toString() {
            return "Filters{" +
                    "filters=" + filters +
                    ", condition=" + condition +
                    '}';
        }
    }

    public static class Filter {
        private final String name;
        private final String filter;

        public Filter(String name, String filter) {
            this.name = name;
            this.filter = filter;
        }

        public String getName() {
            return name;
        }

        public String getFilter() {
            return filter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Filter filter1 = (Filter) o;
            return Objects.equals(name, filter1.name) && Objects.equals(filter, filter1.filter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, filter);
        }

        @Override
        public String toString() {
            return "Filter{" +
                    "name='" + name + '\'' +
                    ", filter='" + filter + '\'' +
                    '}';
        }
    }

    enum ConcatCondition {
        OR, AND
    }

}
