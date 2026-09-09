package by.nhorushko.filterspecification;

import java.util.List;

public class FilterValue {
    private final FilterOperation filterOperation;
    private final List<String> values;

    public FilterValue(FilterOperation filterOperation, List<String> values) {
        this.filterOperation = filterOperation;
        this.values = values;
    }

    public FilterOperation getFilterOperation() {
        return filterOperation;
    }

    public List<String> getValues() {
        return values;
    }
}
