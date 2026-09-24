package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderView;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderViewMapper extends AbsMapEntityToDto<OrderEntity, OrderView> {

    public OrderViewMapper(AbsMapper mapper) {
        super(mapper, OrderEntity.class, OrderView.class);
    }

    @Override
    protected OrderView create(OrderEntity entity) {
        return new OrderView(entity.getId(), entity.getName());
    }
}
