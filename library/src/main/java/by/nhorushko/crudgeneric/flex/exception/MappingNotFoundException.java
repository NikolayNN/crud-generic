package by.nhorushko.crudgeneric.flex.exception;

/**
 * Thrown when no {@code Mapper} or {@code Updater} is registered for the exact pair of classes a
 * mapping call needs. The message names the kind, the direction and both fully qualified class
 * names, e.g. {@code No Updater registered for com.x.OrderUpdateDto -> com.x.OrderEntity}.
 */
public class MappingNotFoundException extends RuntimeException {

    public MappingNotFoundException(String kind, Class<?> from, Class<?> to) {
        super(String.format("No %s registered for %s -> %s", kind, from.getName(), to.getName()));
    }
}
