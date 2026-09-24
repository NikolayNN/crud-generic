package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsFlexMapConfig;
import by.nhorushko.crudgeneric.flex.mapper.Patches;
import by.nhorushko.crudgenerictest.domain.dto.OrderCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderNamePatch;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderMapConfig extends AbsFlexMapConfig<OrderCreateDto, OrderUpdateDto, OrderDto, OrderEntity> {

    public OrderMapConfig(AbsMapper mapper) {
        super(mapper, OrderCreateDto.class, OrderUpdateDto.class, OrderDto.class, OrderEntity.class);
    }

    @Override
    protected OrderEntity toEntity(OrderCreateDto dto) {
        OrderEntity entity = new OrderEntity();
        entity.setName(dto.getName());
        if (dto.getLines() != null) { // null means "no children": keep the entity's empty collection
            entity.setLines(mapper.mapAll(dto.getLines(), OrderLineEntity.class)); // nested children: explicit
        }
        return entity;
    }

    /** Writes the name as is: a null in the DTO clears it (see FlexUpdateIT). */
    @Override
    protected void updateEntity(OrderUpdateDto dto, OrderEntity entity) {
        entity.setName(dto.getName());
    }

    @Override
    protected OrderDto toReadDto(OrderEntity entity) {
        return new OrderDto(entity.getId(), entity.getName());
    }

    @Override
    protected void patches(Patches<OrderEntity> p) {
        p.add(OrderNamePatch.class, (patch, entity) -> entity.setName(patch.name()));
    }
}
