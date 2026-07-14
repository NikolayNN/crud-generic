package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import org.junit.Test;
import org.springframework.data.domain.Sort;

import static by.nhorushko.filterspecification.FilterOperation.CONTAINS;
import static by.nhorushko.filterspecification.FilterOperation.EQUAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class FilterFieldsSortTest {

    private static class Person {
    }

    private final FilterFields<Person> fields = FilterFields.<Person>builder()
            .string("name", CONTAINS)
            .ofLong("userId", "user.id", EQUAL)
            .build();

    @Test
    public void parsesAscAndDesc() {
        assertEquals(Sort.by(Sort.Direction.ASC, "name"), fields.sort("asc#name"));
        assertEquals(Sort.by(Sort.Direction.DESC, "name"), fields.sort("desc#name"));
    }

    @Test
    public void barePropertyIsAscending() {
        assertEquals(Sort.by(Sort.Direction.ASC, "name"), fields.sort("name"));
    }

    @Test
    public void mapsPropertyThroughRegistry() {
        assertEquals(Sort.by(Sort.Direction.DESC, "user.id"), fields.sort("desc#userId"));
    }

    @Test
    public void unknownPropertyPassesThrough() {
        assertEquals(Sort.by(Sort.Direction.ASC, "id"), fields.sort("asc#id"));
    }

    @Test
    public void legacyPlusMinusSyntaxIsRejected() {
        assertThrows(FilterValidationException.class, () -> fields.sort("+name"));
        assertThrows(FilterValidationException.class, () -> fields.sort("-name"));
    }

    @Test
    public void malformedExpressionIsRejected() {
        assertThrows(FilterValidationException.class, () -> fields.sort("up#name"));
        assertThrows(FilterValidationException.class, () -> fields.sort("asc#"));
        assertThrows(FilterValidationException.class, () -> fields.sort(""));
        assertThrows(FilterValidationException.class, () -> fields.sort(null));
    }
}
