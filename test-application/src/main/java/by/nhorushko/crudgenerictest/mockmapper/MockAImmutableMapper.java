package by.nhorushko.crudgenerictest.mockmapper;

import by.nhorushko.crudgeneric.v2.mapper.AbsMapperBase;
import by.nhorushko.crudgenerictest.domain.dto.MockAImmutableDto;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * Mapper for {@link MockAImmutableDto}.
 * <p>
 * Extends {@link AbsMapperBase} directly (rather than {@link by.nhorushko.crudgeneric.v2.mapper.AbsMapperDto})
 * because {@code MockAEntity} implements the legacy v1 {@code AbstractEntity} interface, which does not
 * satisfy the {@code ENTITY extends by.nhorushko.crudgeneric.v2.domain.AbstractEntity} bound on
 * {@code AbsMapperDto}. The Converter is registered manually in the constructor,
 * which is the behavior {@code AbsMapperDto.configureMapper()} would have provided.
 * Being a subclass of {@link AbsMapperBase} is sufficient for
 * {@link by.nhorushko.crudgeneric.flex.config.AbsMapperEagerInitPostProcessor} to detect this bean.
 * </p>
 */
@Service
public class MockAImmutableMapper extends AbsMapperBase<MockAEntity, MockAImmutableDto> {

    public MockAImmutableMapper(ModelMapper modelMapper) {
        super(modelMapper, MockAEntity.class, MockAImmutableDto.class);
        modelMapper
                .createTypeMap(MockAEntity.class, MockAImmutableDto.class)
                .setConverter(context -> {
                    MockAEntity entity = context.getSource();
                    return new MockAImmutableDto(entity.getId(), entity.getName());
                });
    }

    public MockAImmutableDto toDto(MockAEntity entity) {
        return map(entity, dtoClass);
    }
}
