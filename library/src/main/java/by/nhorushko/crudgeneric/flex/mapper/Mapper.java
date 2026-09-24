package by.nhorushko.crudgeneric.flex.mapper;

import java.util.Objects;
import java.util.function.Function;

/**
 * Explicit conversion of a {@code FROM} instance into a new {@code TO} instance.
 * <p>
 * The pair of classes is declared, not inferred from generics: {@link MapperRegistry} keys every
 * mapper by {@code (fromClass(), toClass())} and finds it by the exact runtime class of the source.
 * </p>
 *
 * @param <FROM> the source type
 * @param <TO>   the produced type
 */
public interface Mapper<FROM, TO> {

    Class<FROM> fromClass();

    Class<TO> toClass();

    TO map(FROM from);

    /**
     * Wraps a function as a mapper for the given pair of classes.
     */
    static <F, T> Mapper<F, T> of(Class<F> from, Class<T> to, Function<F, T> fn) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fn, "fn");
        return new Mapper<>() {
            @Override
            public Class<F> fromClass() {
                return from;
            }

            @Override
            public Class<T> toClass() {
                return to;
            }

            @Override
            public T map(F source) {
                return fn.apply(source);
            }
        };
    }
}
