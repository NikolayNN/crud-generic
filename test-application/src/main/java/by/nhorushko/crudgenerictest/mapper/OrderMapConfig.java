package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.composite.AbsFlexMapConfigDefault;
import by.nhorushko.crudgenerictest.domain.dto.OrderCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderMapConfig extends AbsFlexMapConfigDefault<OrderCreateDto, OrderUpdateDto, OrderDto, OrderEntity> {

    public OrderMapConfig(AbsModelMapper mapper) {
        super(mapper, OrderCreateDto.class, OrderUpdateDto.class, OrderDto.class, OrderEntity.class);
    }

    @Override
    protected OrderDto createReadDtoFromEntity(AbsModelMapper mapper, OrderEntity entity) {
        return new OrderDto(entity.getId(), entity.getName());
    }
}
