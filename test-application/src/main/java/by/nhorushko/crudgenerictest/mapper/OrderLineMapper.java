package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapDtoToEntity;
import by.nhorushko.crudgenerictest.domain.dto.OrderLineDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import org.springframework.stereotype.Component;

/**
 * Nested order lines: {@code OrderMapConfig.toEntity} maps them through this mapper, which
 * normalises the sentinel id 0 of a new line to null. Create direction only — nothing maps lines
 * back to DTOs.
 */
@Component
public class OrderLineMapper extends AbsMapDtoToEntity<OrderLineDto, OrderLineEntity> {

    public OrderLineMapper(AbsMapper mapper) {
        super(mapper, OrderLineDto.class, OrderLineEntity.class);
    }

    @Override
    protected OrderLineEntity create(OrderLineDto from) {
        OrderLineEntity line = new OrderLineEntity();
        line.setId(from.getId());
        line.setTitle(from.getTitle());
        return line;
    }
}
