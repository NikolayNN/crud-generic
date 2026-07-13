package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapDtoToEntity;
import by.nhorushko.crudgenerictest.domain.dto.OrderLineDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import org.springframework.stereotype.Component;

/**
 * Registers the nested order-line mappings used when order DTOs carry line DTOs
 * (cascade create/update through the parent order).
 */
@Component
public class OrderLineMapConfig {

    public OrderLineMapConfig(AbsModelMapper mapper) {
        new AbsMapDtoToEntity<>(mapper, OrderLineDto.class, OrderLineEntity.class) {
        };
        new AbsMapEntityToDto<>(mapper, OrderLineEntity.class, OrderLineDto.class) {
            @Override
            protected OrderLineDto create(OrderLineEntity from) {
                return new OrderLineDto(from.getId(), from.getTitle());
            }
        };
    }
}
