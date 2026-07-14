package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import org.junit.Test;

import java.util.Optional;

import static by.nhorushko.filterspecification.FilterOperation.*;
import static org.junit.Assert.*;

public class FilterFieldsToSpecificationTest {

    private static class Person {
    }

    private enum Status {ACTIVE, ARCHIVED}

    private final FilterFields<Person> fields = FilterFields.<Person>builder()
            .string("name", CONTAINS)
            .ofLong("userId", "user.id", EQUAL)
            .instant("createdAt", BETWEEN, IS_NULL, NOT_NULL)
            .ofEnum("status", Status.class, EQUAL)
            .custom("special", filter -> (root, query, cb) -> cb.conjunction())
            .build();

    private static PageFilterRequest.Filter filter(String name, String value) {
        return new PageFilterRequest.Filter(name, value);
    }

    @Test
    public void buildsSpecificationForAllowedOperation() {
        assertTrue(fields.toSpecification(filter("name", "like#john")).isPresent());
        assertTrue(fields.toSpecification(filter("userId", "eq#5")).isPresent());
        assertTrue(fields.toSpecification(
                filter("createdAt", "btn#2024-01-01T00:00:00Z,2024-12-31T00:00:00Z")).isPresent());
        assertTrue(fields.toSpecification(filter("createdAt", "en#null")).isPresent());
        assertTrue(fields.toSpecification(filter("status", "eq#ACTIVE")).isPresent());
    }

    @Test
    public void blankValueIsSkipped() {
        assertEquals(Optional.empty(), fields.toSpecification(filter("name", null)));
        assertEquals(Optional.empty(), fields.toSpecification(filter("name", "  ")));
        assertEquals(Optional.empty(), fields.toSpecification(filter("special", "")));
    }

    @Test
    public void unknownFieldIsRejected() {
        FilterValidationException e = assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("nope", "eq#1")));
        assertTrue(e.getMessage().contains("nope"));
    }

    @Test
    public void disallowedOperationIsRejected() {
        FilterValidationException e = assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("name", "eq#john")));
        assertTrue(e.getMessage().contains("name"));
    }

    @Test
    public void malformedOperationIsRejected() {
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("name", "garbage")));
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("name", "zz#john")));
    }

    @Test
    public void unconvertibleValueIsRejected() {
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("userId", "eq#abc")));
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("status", "eq#NO_SUCH")));
        assertThrows(FilterValidationException.class,
                () -> fields.toSpecification(filter("createdAt", "btn#not-a-date,2024-12-31T00:00:00Z")));
    }

    @Test
    public void customEntryDelegatesToFactory() {
        assertTrue(fields.toSpecification(filter("special", "eq#anything")).isPresent());
    }
}
