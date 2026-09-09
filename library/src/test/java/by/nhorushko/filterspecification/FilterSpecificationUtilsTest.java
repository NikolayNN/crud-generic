package by.nhorushko.filterspecification;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Set;

import static org.junit.Assert.*;

public class FilterSpecificationUtilsTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void isBlank() {
        assertFalse(FilterSpecificationUtils.isBlank("eq#123"));
        assertFalse(FilterSpecificationUtils.isBlank("in#123,321"));
        assertTrue(FilterSpecificationUtils.isBlank(null));
        assertTrue(FilterSpecificationUtils.isBlank("    "));
        assertTrue(FilterSpecificationUtils.isBlank("eq"));
        assertTrue(FilterSpecificationUtils.isBlank("eq#"));

    }

    @Test
    public void getAllValues() {
        assertEquals(new String[]{"123"}, FilterSpecificationUtils.getAllValues("eq#123"));
        assertEquals(new String[]{"1", "2", "3"}, FilterSpecificationUtils.getAllValues("in#1,2,3"));
    }

    @Test
    public void getFirstValue() {
        assertEquals("123", FilterSpecificationUtils.getFirstValue("eq#123"));
        assertEquals("1", FilterSpecificationUtils.getFirstValue("in#1,2,3"));
    }

    @Test
    public void getFirstLongValue() {
        assertEquals(123L, FilterSpecificationUtils.getFirstLongValue("eq#123"));
        assertEquals(1L, FilterSpecificationUtils.getFirstLongValue("in#1,2,3"));
    }

    @Test
    public void getOperation() {
        assertEquals(FilterOperation.EQUAL, FilterSpecificationUtils.getOperation("eq#123"));
        assertEquals(FilterOperation.IN, FilterSpecificationUtils.getOperation("in#1,2,3"));
    }

    @Test
    public void isAvailableOperation() {
        assertTrue(FilterSpecificationUtils.isAvailableOperation("eq#123", Set.of(FilterOperation.IN, FilterOperation.EQUAL)));
        assertTrue(FilterSpecificationUtils.isAvailableOperation(null, Set.of(FilterOperation.IN, FilterOperation.EQUAL)));
        assertFalse(FilterSpecificationUtils.isAvailableOperation("eq#123", Set.of(FilterOperation.IN)));
    }

    @Test
    public void checkFilterOperation() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Filter: 'eq#123', expect operations: '[in]', but was 'eq'");
        FilterSpecificationUtils.checkFilterOperation("eq#123", Set.of(FilterOperation.IN));
    }

    @Test
    public void checkFilterOperation_ok() {
        FilterSpecificationUtils.checkFilterOperation("eq#123", Set.of(FilterOperation.IN, FilterOperation.EQUAL));
        FilterSpecificationUtils.checkFilterOperation(null, Set.of(FilterOperation.IN, FilterOperation.EQUAL));
    }

    @Test
    public void buildFilter() {
        String actual = FilterSpecificationUtils.buildFilter(FilterOperation.IN, "1", "2", "3");
        assertEquals("in#1,2,3", actual);

        actual = FilterSpecificationUtils.buildFilter(FilterOperation.EQUAL, 1);
        assertEquals("eq#1", actual);

        actual = FilterSpecificationUtils.buildFilter(FilterOperation.BETWEEN, 1, 2);
        assertEquals("btn#1,2", actual);
    }

    @Test
    public void buildFilter_empty() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("can't be build filter params length is 0");

        FilterSpecificationUtils.buildFilter(FilterOperation.IN);
    }

    @Test
    public void buildFilter_expectTwoValues() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("can't build filter for operation: btn expect 2 params, but: 3");

        FilterSpecificationUtils.buildFilter(FilterOperation.BETWEEN, 1, 2, 3);
    }

    @Test
    public void buildFilter_expectOneValue() {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("can't build filter for operation: eq expect 1 params, but: 2");

        FilterSpecificationUtils.buildFilter(FilterOperation.EQUAL, 1, 2);
    }
}
