package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import lombok.Value;

/**
 * Immutable (@Value — no no-arg constructor): built only by {@code OrderViewMapper}.
 * {@code MapperRegistryLazyInitIT} maps into it under global lazy init.
 */
@Value
public class OrderView implements AbstractDto<Long> {
    Long id;
    String name;
}
