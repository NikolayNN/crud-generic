package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import lombok.Value;

/**
 * Immutable, Lombok-{@link Value} DTO without a no-arg constructor.
 * Used to verify that crud-generic registers ModelMapper Converters before
 * any consumer calls {@code modelMapper.map(...)} — including under
 * {@code spring.main.lazy-initialization=true}.
 */
@Value
public class MockAImmutableDto implements AbsBaseDto {
    Long id;
    String name;
}
