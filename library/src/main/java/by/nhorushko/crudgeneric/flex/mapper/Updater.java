package by.nhorushko.crudgeneric.flex.mapper;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Explicit in-place write of a {@code FROM} instance onto an existing {@code TO} instance, typically
 * an update DTO or a PATCH body onto a managed entity. Only what the updater writes changes.
 * <p>
 * Keyed in {@link MapperRegistry} by {@code (fromClass(), toClass())}, looked up by the exact
 * runtime classes of both instances.
 * </p>
 *
 * @param <FROM> the source type
 * @param <TO>   the type written into
 */
public interface Updater<FROM, TO> {

    Class<FROM> fromClass();

    Class<TO> toClass();

    void update(FROM from, TO into);

    /**
     * Wraps a function as an updater for the given pair of classes.
     */
    static <F, T> Updater<F, T> of(Class<F> from, Class<T> to, BiConsumer<F, T> fn) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(fn, "fn");
        return new Updater<>() {
            @Override
            public Class<F> fromClass() {
                return from;
            }

            @Override
            public Class<T> toClass() {
                return to;
            }

            @Override
            public void update(F source, T into) {
                fn.accept(source, into);
            }
        };
    }
}
