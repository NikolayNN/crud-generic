package by.nhorushko.filterspecification;

public class SearchCriteria {
    private final String key;
    private final FilterOperation operation;
    private final Object value;

    public SearchCriteria(String key, FilterOperation operation, Object value) {
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public FilterOperation getOperation() {
        return operation;
    }

    public Object getValue() {
        return value;
    }
}
