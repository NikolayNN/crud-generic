package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.OrderCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceCRUD extends AbsFlexServiceCRUD<Long, OrderEntity, OrderDto, OrderUpdateDto, OrderCreateDto, OrderRepository> {

    public OrderServiceCRUD(AbsMapper mapper, OrderRepository repository) {
        super(mapper, repository, OrderEntity.class, OrderDto.class, OrderUpdateDto.class, OrderCreateDto.class);
    }

    /**
     * Renames the order through {@code changeEntity}: a change written in code, no request body, so
     * only the after-update hooks run.
     */
    public OrderDto rename(Long id, String name) {
        return changeEntity(id, order -> order.setName(name));
    }
}
