package by.nhorushko.crudgeneric.v2.pageable;

import by.nhorushko.filterspecification.Converters;
import by.nhorushko.filterspecification.FilterCriteria;
import by.nhorushko.filterspecification.FilterOperation;
import by.nhorushko.filterspecification.FilterSpecifications;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.chrono.ChronoLocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public abstract class AbsFilterSpecification<ENTITY> {

    protected final Map<String, Context> entityContext = new HashMap<>();
    protected final Converters converters;

    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, Integer> integerFilterSpecifications;
    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, Long> longFilterSpecifications;
    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, String> stringFilterSpecifications;
    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, Instant> instantFilterSpecifications;
    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, Boolean> booleanFilterSpecifications;
    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, Float> floatFilterSpecifications;
    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, Double> doubleFilterSpecifications;

    @Lazy
    @Autowired
    private FilterSpecifications<ENTITY, ChronoLocalDateTime<?>> localDateTimeFilterSpecifications;


    public AbsFilterSpecification(Converters converters) {
        this.converters = converters;
        this.setupFieldPaths(this.entityContext);
    }

    public String handleSort(PageFilterRequest request) {
        String sort = request.getSort();
        sort = sort.replace("asc#", "+");
        sort = sort.replace("desc#", "-");
        String sign = "+";
        if (sort.startsWith("+")) {
            sort = sort.substring(1);
        }
        if (sort.startsWith("-")) {
            sort = sort.substring(1);
            sign = "-";
        }
        for (String s : entityContext.keySet()) {
            if (s.equals(sort)) {
                sort = entityContext.get(s).path;
                break;
            }
        }
        return sign + sort;
    }

    protected abstract void setupFieldPaths(Map<String, Context> entityContext);

    public Specification<ENTITY> getIntegerSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, integerFilterSpecifications);
    }

    public Specification<ENTITY> getLongSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, longFilterSpecifications);
    }

    public Specification<ENTITY> getStringSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, stringFilterSpecifications);
    }

    public Specification<ENTITY> getInstantSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, instantFilterSpecifications);
    }

    public Specification<ENTITY> getBooleanSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, booleanFilterSpecifications);
    }

    public Specification<ENTITY> getFloatSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, floatFilterSpecifications);
    }

    public Specification<ENTITY> getDoubleSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, doubleFilterSpecifications);
    }

    public Specification<ENTITY> getLocalDateTimeSpecification(PageFilterRequest.Filter filter) {
        return buildSpecification(filter, localDateTimeFilterSpecifications);
    }

    private String getPathByName(String propertyName) {
        Context context = this.entityContext.get(propertyName);
        if (context == null) {
            throw new NoSuchElementException(String.format("Context do not found by property name: %s", propertyName));
        }
        return context.path;
    }

    protected <T extends Comparable<T>> Specification<ENTITY> buildSpecification(PageFilterRequest.Filter filter,
                                                                                 FilterSpecifications<ENTITY, T> specification) {
        String fieldName = this.getPathByName(filter.getName());
        Function<String, T> converterFunction = converters.getFunction(entityContext.get(filter.getName()).clazz);
        return StringUtils.isNotBlank(filter.getFilter()) ?
                this.buildSpecification(fieldName, filter.getFilter(), converterFunction, specification)
                : this.alwaysTrue(fieldName, specification);
    }

    private <T extends Comparable<T>> Specification<ENTITY> buildSpecification(String fieldName,
                                                                               String filterValue,
                                                                               Function<String, T> converter,
                                                                               FilterSpecifications<ENTITY, T> specifications) {
        FilterCriteria<T> criteria = new FilterCriteria(fieldName, filterValue, converter);
        return specifications.getSpecification(criteria.getOperation()).apply(criteria);
    }

    private <T extends Comparable<T>> Specification<ENTITY> alwaysTrue(String fieldName,
                                                                       FilterSpecifications<ENTITY, T> specifications) {
        Function<String, T> defFunction = this.converters.getFunction(String.class);
        return (Specification) specifications.getSpecification(FilterOperation.ALWAYS_TRUE)
                .apply(new FilterCriteria(fieldName, "always_true#TRUE", defFunction));
    }

    public static class Context {
        final String path;
        final Class<?> clazz;

        public Context(String path, Class<?> clazz) {
            this.path = path;
            this.clazz = clazz;
        }
    }
}
