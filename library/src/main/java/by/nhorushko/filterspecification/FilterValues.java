package by.nhorushko.filterspecification;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public class FilterValues {
    public static FilterValue parse(String values) {
        Validate.notEmpty(values, "Filter criteria can't be empty");

        String[] filterSplit = StringUtils.split(values, FilterSpecificationConstants.FIELD_FILTER);
        if (filterSplit.length != 2) {
            throw new IllegalArgumentException(String.format("More than one or no separator '%s' found in filter: %s", FilterSpecificationConstants.FIELD_FILTER, values));
        }
        String[] operationValues = StringUtils.split(filterSplit[1], FilterSpecificationConstants.VALUES);
        if (operationValues.length < 1) {
            throw new IllegalArgumentException("Operation value can't be empty");
        }
        return new FilterValue(FilterOperation.fromValue(filterSplit[0]), Arrays.asList(operationValues));
    }
}
