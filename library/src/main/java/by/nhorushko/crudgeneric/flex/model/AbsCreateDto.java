package by.nhorushko.crudgeneric.flex.model;

/**
 * Interface for marking DTOs (Data Transfer Objects) specifically intended for creating new entities.
 * <p>
 * Implementing this interface signifies that the DTO is designed to carry data necessary for the creation
 * of a new entity within the system. This interface extends {@link AbsBaseDto}, indicating that the DTO
 * does not necessarily contain an identifier at the time of creation, as IDs are typically assigned to
 * entities upon their persistence in a database.
 * </p>
 */
public interface AbsCreateDto extends AbsBaseDto {
}
