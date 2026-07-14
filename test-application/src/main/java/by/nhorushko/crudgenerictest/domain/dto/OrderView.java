package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import lombok.Value;

/**
 * Immutable (@Value — no no-arg constructor): mapping OrderEntity -> OrderView only works
 * through the converter registered by OrderViewMapper. The eager-init tests rely on the
 * reflective fallback failing for this type when registration did not happen.
 */
@Value
public class OrderView implements AbstractDto<Long> {
    Long id;
    String name;
}
