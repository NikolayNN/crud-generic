package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest.FilterGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder class for constructing nested filter group structures that can be used to
 * create complex query conditions.
 * <p>
 * This builder supports chaining methods to add filters with AND/OR conditions and
 * can handle nesting of these conditions to any depth.
 * Usage example:
 *
 * <pre>
 * {@code
 *  FilterGroupBuilder builder = new FilterGroupBuilder();
 *  FilterGroup group = builder
 *      .or(
 *          FilterGroupBuilder.and(new Filter("field1", "value1"), new Filter("field2", "value2")),
 *          FilterGroupBuilder.or(new Filter("field3", "value3"), new Filter("field4", "value4"))
 *      )
 *      .build();
 *
 *  // Now 'group' can be used to create a PageFilterRequest or any other context where a complex filter structure is needed.
 *  }
 * </pre>
 */
public class FilterGroupBuilder {

    private final List<FilterGroup> groups = new ArrayList<>();
    private PageFilterRequest.ConcatCondition currentCondition = PageFilterRequest.ConcatCondition.OR;


    public FilterGroupBuilder() {
    }


    public FilterGroupBuilder and(FilterGroup... groups) {
        this.currentCondition = PageFilterRequest.ConcatCondition.AND;
        this.groups.addAll(Arrays.asList(groups));
        return this;
    }

    public FilterGroupBuilder or(FilterGroup... groups) {
        this.currentCondition = PageFilterRequest.ConcatCondition.OR;
        this.groups.addAll(Arrays.asList(groups));
        return this;
    }

    public FilterGroup build() {
        if (groups.isEmpty()) {
            throw new IllegalStateException("No filters or groups have been added to the builder.");
        }
        return new FilterGroup(new ArrayList<>(), currentCondition, groups);
    }

    public static FilterGroup and(PageFilterRequest.Filter... filters) {
        return new FilterGroup(Arrays.asList(filters), PageFilterRequest.ConcatCondition.AND);
    }

    public static FilterGroup or(PageFilterRequest.Filter... filters) {
        return new FilterGroup(Arrays.asList(filters), PageFilterRequest.ConcatCondition.OR);
    }

    public static FilterGroup filter(PageFilterRequest.Filter filter) {
        return new FilterGroup(List.of(filter), PageFilterRequest.ConcatCondition.AND);
    }
}
