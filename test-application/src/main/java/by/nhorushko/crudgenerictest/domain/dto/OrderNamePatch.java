package by.nhorushko.crudgenerictest.domain.dto;

/**
 * PATCH body that renames an order. No id: the id comes from the path.
 */
public record OrderNamePatch(String name) {
}
