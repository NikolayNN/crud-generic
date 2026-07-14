package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import by.nhorushko.filterspecification.Converters;
import by.nhorushko.filterspecification.FilterCriteria;
import by.nhorushko.filterspecification.FilterOperation;
import by.nhorushko.filterspecification.FilterSpecificationUtils;
import by.nhorushko.filterspecification.FilterSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Immutable registry of filterable fields — the single place a filterable
 * field of a paged endpoint is declared. Each entry records the external
 * filter name, the entity path, the value converter and the allowed
 * operations; the registry validates incoming filters and builds
 * {@link Specification}s and sort mappings from the same declaration.
 * <p>
 * Typed builder methods carry built-in converters. Only {@link Builder#field}
 * needs a {@link Converters} instance (use {@link #builder(Converters)}).
 */
public final class FilterFields<ENTITY> {

    private final Map<String, Entry<ENTITY>> entries;
    @SuppressWarnings("rawtypes")
    private final FilterSpecifications specifications = new FilterSpecifications();

    private FilterFields(Map<String, Entry<ENTITY>> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public static <E> Builder<E> builder() {
        return new Builder<>(null);
    }

    public static <E> Builder<E> builder(Converters converters) {
        Objects.requireNonNull(converters, "converters");
        return new Builder<>(converters);
    }

    /**
     * Validates the filter against this registry and builds a specification.
     * Blank values produce {@link Optional#empty()} (the filter is skipped).
     *
     * @throws FilterValidationException on unknown field, disallowed or
     *                                   unparseable operation, or a value that fails conversion
     */
    public Optional<Specification<ENTITY>> toSpecification(PageFilterRequest.Filter filter) {
        Entry<ENTITY> entry = entries.get(filter.getName());
        if (entry == null) {
            throw new FilterValidationException(String.format("Unknown filter field: '%s'", filter.getName()));
        }
        String value = filter.getFilter();
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        if (entry.customFactory != null) {
            return Optional.of(entry.customFactory.apply(filter));
        }
        FilterOperation operation = FilterSpecificationUtils.getOperation(value);
        if (operation == null || !entry.operations.contains(operation)) {
            throw new FilterValidationException(String.format(
                    "Filter: '%s', expect operations: '%s', but was '%s'",
                    filter.getName(), entry.operations, operation));
        }
        return Optional.of(buildSpecification(entry, value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Specification<ENTITY> buildSpecification(Entry<ENTITY> entry, String filterValue) {
        FilterCriteria criteria;
        try {
            criteria = new FilterCriteria(entry.path, filterValue, entry.converter);
        } catch (RuntimeException e) {
            throw new FilterValidationException(String.format(
                    "Filter '%s': can't parse value '%s': %s", entry.name, filterValue, e.getMessage()), e);
        }
        return (Specification<ENTITY>) specifications.getSpecification(criteria.getOperation()).apply(criteria);
    }

    /**
     * Parses a sort expression — {@code asc#field}, {@code desc#field} or a
     * bare {@code field} (ascending) — and maps the property through this
     * registry ({@code userId} → {@code user.id}). Unknown properties pass
     * through unchanged. The legacy {@code +field}/{@code -field} syntax is
     * rejected: {@code +} is URL-decoded to a space and caused real bugs.
     *
     * @throws FilterValidationException on blank, legacy or malformed expressions
     */
    public Sort sort(String sortExpression) {
        if (sortExpression == null || sortExpression.trim().isEmpty()) {
            throw new FilterValidationException("Sort expression is blank");
        }
        String expression = sortExpression.trim();
        if (expression.startsWith("+") || expression.startsWith("-")) {
            throw new FilterValidationException(String.format(
                    "Legacy sort syntax '%s' is not supported; use asc#field or desc#field", expression));
        }
        Sort.Direction direction = Sort.Direction.ASC;
        String property = expression;
        if (expression.startsWith("asc#")) {
            property = expression.substring("asc#".length());
        } else if (expression.startsWith("desc#")) {
            direction = Sort.Direction.DESC;
            property = expression.substring("desc#".length());
        } else if (expression.contains("#")) {
            throw new FilterValidationException(String.format("Malformed sort expression: '%s'", expression));
        }
        if (property.isEmpty()) {
            throw new FilterValidationException(String.format("Malformed sort expression: '%s'", expression));
        }
        Entry<ENTITY> entry = entries.get(property);
        return Sort.by(direction, entry != null ? entry.path : property);
    }

    static final class Entry<ENTITY> {
        final String name;
        final String path;
        final Set<FilterOperation> operations;
        final Function<String, ? extends Comparable<?>> converter;
        final Function<PageFilterRequest.Filter, Specification<ENTITY>> customFactory;

        Entry(String name, String path, Set<FilterOperation> operations,
              Function<String, ? extends Comparable<?>> converter,
              Function<PageFilterRequest.Filter, Specification<ENTITY>> customFactory) {
            this.name = name;
            this.path = path;
            this.operations = operations;
            this.converter = converter;
            this.customFactory = customFactory;
        }
    }

    public static final class Builder<ENTITY> {

        private final Converters converters;
        private final Map<String, Entry<ENTITY>> entries = new LinkedHashMap<>();

        private Builder(Converters converters) {
            this.converters = converters;
        }

        public Builder<ENTITY> string(String name, FilterOperation... operations) {
            return string(name, name, operations);
        }

        public Builder<ENTITY> string(String name, String path, FilterOperation... operations) {
            return typed(name, path, s -> s, operations);
        }

        public Builder<ENTITY> ofLong(String name, FilterOperation... operations) {
            return ofLong(name, name, operations);
        }

        public Builder<ENTITY> ofLong(String name, String path, FilterOperation... operations) {
            return typed(name, path, Long::valueOf, operations);
        }

        public Builder<ENTITY> ofInteger(String name, FilterOperation... operations) {
            return ofInteger(name, name, operations);
        }

        public Builder<ENTITY> ofInteger(String name, String path, FilterOperation... operations) {
            return typed(name, path, Integer::valueOf, operations);
        }

        public Builder<ENTITY> ofDouble(String name, FilterOperation... operations) {
            return ofDouble(name, name, operations);
        }

        public Builder<ENTITY> ofDouble(String name, String path, FilterOperation... operations) {
            return typed(name, path, Double::valueOf, operations);
        }

        public Builder<ENTITY> ofFloat(String name, FilterOperation... operations) {
            return ofFloat(name, name, operations);
        }

        public Builder<ENTITY> ofFloat(String name, String path, FilterOperation... operations) {
            return typed(name, path, Float::valueOf, operations);
        }

        public Builder<ENTITY> ofBoolean(String name, FilterOperation... operations) {
            return ofBoolean(name, name, operations);
        }

        public Builder<ENTITY> ofBoolean(String name, String path, FilterOperation... operations) {
            return typed(name, path, Boolean::valueOf, operations);
        }

        public Builder<ENTITY> instant(String name, FilterOperation... operations) {
            return instant(name, name, operations);
        }

        public Builder<ENTITY> instant(String name, String path, FilterOperation... operations) {
            return typed(name, path, Instant::parse, operations);
        }

        public Builder<ENTITY> ofLocalDate(String name, FilterOperation... operations) {
            return ofLocalDate(name, name, operations);
        }

        public Builder<ENTITY> ofLocalDate(String name, String path, FilterOperation... operations) {
            return typed(name, path, LocalDate::parse, operations);
        }

        public Builder<ENTITY> ofLocalDateTime(String name, FilterOperation... operations) {
            return ofLocalDateTime(name, name, operations);
        }

        public Builder<ENTITY> ofLocalDateTime(String name, String path, FilterOperation... operations) {
            return typed(name, path, LocalDateTime::parse, operations);
        }

        public <E extends Enum<E>> Builder<ENTITY> ofEnum(String name, Class<E> enumClass,
                                                          FilterOperation... operations) {
            return ofEnum(name, name, enumClass, operations);
        }

        public <E extends Enum<E>> Builder<ENTITY> ofEnum(String name, String path, Class<E> enumClass,
                                                          FilterOperation... operations) {
            Objects.requireNonNull(enumClass, "enumClass");
            return typed(name, path, value -> Enum.valueOf(enumClass, value), operations);
        }

        /**
         * Field of any other type; the converter must be registered in the
         * application's {@link Converters} subclass and the builder must be
         * created via {@link FilterFields#builder(Converters)}.
         */
        public Builder<ENTITY> field(String name, String path, Class<?> type, FilterOperation... operations) {
            Objects.requireNonNull(type, "type");
            if (converters == null) {
                throw new IllegalStateException(String.format(
                        "Field '%s': builder was created without Converters; use FilterFields.builder(converters)", name));
            }
            Function<String, ? extends Comparable<?>> converter = converters.getFunction(type);
            if (converter == null) {
                throw new IllegalStateException(String.format(
                        "Field '%s': no converter registered for type %s", name, type.getName()));
            }
            return typed(name, path, converter, operations);
        }

        /**
         * Escape hatch: the factory receives the raw filter and builds the
         * {@link Specification} itself. Operations are not validated.
         */
        public Builder<ENTITY> custom(String name,
                                      Function<PageFilterRequest.Filter, Specification<ENTITY>> factory) {
            Objects.requireNonNull(factory, "factory");
            put(new Entry<>(name, name, Set.of(), null, factory));
            return this;
        }

        private Builder<ENTITY> typed(String name, String path,
                                      Function<String, ? extends Comparable<?>> converter,
                                      FilterOperation... operations) {
            if (operations.length == 0) {
                throw new IllegalStateException(String.format(
                        "Field '%s' must declare at least one operation", name));
            }
            put(new Entry<>(name, path, Set.of(operations), converter, null));
            return this;
        }

        private void put(Entry<ENTITY> entry) {
            Objects.requireNonNull(entry.name, "name");
            Objects.requireNonNull(entry.path, "path");
            if (entries.putIfAbsent(entry.name, entry) != null) {
                throw new IllegalStateException(String.format("Duplicate filter field: '%s'", entry.name));
            }
        }

        public FilterFields<ENTITY> build() {
            return new FilterFields<>(entries);
        }
    }
}
