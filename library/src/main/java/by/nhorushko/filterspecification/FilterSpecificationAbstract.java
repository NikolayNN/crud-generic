package by.nhorushko.filterspecification;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class FilterSpecificationAbstract<ENTITY> {

    protected Map<String, String> entityFieldPaths = new HashMap<>();

    /**
     * Converter Functions
     */
    @Autowired
    protected Converters converters;

    public FilterSpecificationAbstract() {
        setupFieldPaths(entityFieldPaths);
    }

    protected void setupFieldPaths(Map<String, String> entityFieldPaths) {

    }

    public String getPathByName(String propertyName) {
        return entityFieldPaths.getOrDefault(propertyName, propertyName);
    }

    public Map<String, String> getEntityFieldPaths() {
        return entityFieldPaths;
    }

    /**
     * Generic method to return {@link Specification} for Entity {@link ENTITY}
     *
     * @param fieldName
     * @param filterValue
     * @param converter
     * @param specifications
     * @return
     */
    protected <T extends Comparable<T>> Specification<ENTITY> getSpecification(String fieldName,
                                                                               String filterValue,
                                                                               Function<String, T> converter,
                                                                               FilterSpecifications<ENTITY, T> specifications) {
        fieldName = getPathByName(delFilterSuffix(fieldName));
        if (StringUtils.isNotBlank(filterValue)) {
            return buildSpecification(fieldName, filterValue, converter, specifications);
        }
        return alwaysTrue(fieldName, specifications);
    }

    private <T extends Comparable<T>> Specification<ENTITY> buildSpecification(String fieldName, String filterValue, Function<String, T> converter, FilterSpecifications<ENTITY, T> specifications) {
        FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, converter);
        return specifications.getSpecification(criteria.getOperation()).apply(criteria);
    }

    private <T extends Comparable<T>> Specification<ENTITY> alwaysTrue(String fieldName, FilterSpecifications<ENTITY, T> specifications) {
        Function<String, T> defFunction = converters.getFunction(String.class);
        return specifications.getSpecification(FilterOperation.ALWAYS_TRUE)
                .apply(new FilterCriteria<>(fieldName, "always_true#TRUE", defFunction));
    }

    private String delFilterSuffix(String s) {
        return StringUtils.removeEndIgnoreCase(s, FilterSpecificationConstants.FILTER_SUFFIX);
    }
}
