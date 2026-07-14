package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.filterspecification.Converters;
import org.junit.Test;

import java.math.BigDecimal;

import static by.nhorushko.filterspecification.FilterOperation.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class FilterFieldsBuilderTest {

    /** ENTITY type token for readability; never instantiated. */
    private static class Person {
    }

    private enum Status {ACTIVE, ARCHIVED}

    private static Converters convertersWithBigDecimal() {
        return new Converters() {
            @Override
            public void addConverters() {
                map.put(BigDecimal.class, BigDecimal::new);
            }
        };
    }

    @Test
    public void buildsRegistryWithEveryTypedMethod() {
        FilterFields<Person> fields = FilterFields.<Person>builder()
                .string("name", CONTAINS)
                .string("description", "details.description", CONTAINS)
                .ofLong("userId", "user.id", EQUAL)
                .ofInteger("count", GREATER_THAN)
                .ofDouble("weight", LESS_THAN)
                .ofFloat("ratio", LESS_THAN)
                .ofBoolean("active", EQUAL)
                .instant("createdAt", BETWEEN)
                .ofLocalDate("day", EQUAL)
                .ofLocalDateTime("startedAt", GREATER_THAN)
                .ofEnum("status", Status.class, EQUAL, IN)
                .ofEnum("subStatus", "sub.status", Status.class, EQUAL)
                .custom("special", filter -> (root, query, cb) -> cb.conjunction())
                .build();
        assertNotNull(fields);
    }

    @Test
    public void fieldEntryUsesRegisteredConverter() {
        FilterFields<Person> fields = FilterFields.<Person>builder(convertersWithBigDecimal())
                .field("score", "rating.score", BigDecimal.class, GREATER_THAN, LESS_THAN)
                .build();
        assertNotNull(fields);
    }

    @Test
    public void typedEntryWithoutOperationsIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder().string("name"));
    }

    @Test
    public void duplicateNameIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder()
                        .string("name", CONTAINS)
                        .string("name", EQUAL));
    }

    @Test
    public void fieldEntryWithoutConvertersIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder()
                        .field("score", "rating.score", BigDecimal.class, GREATER_THAN));
    }

    @Test
    public void fieldEntryWithoutRegisteredConverterIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> FilterFields.<Person>builder(convertersWithBigDecimal())
                        .field("headers", "req.headers", StringBuilder.class, EQUAL));
    }
}
