package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgenerictest.domain.dto.OrderView;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class EagerTypeMapRegistrationEagerContextTest {

    @Autowired
    private ModelMapper modelMapper;

    @Test
    void modelMapperMap_immutableDto_succeedsInEagerContext() {
        OrderEntity entity = new OrderEntity();
        entity.setId(7L);
        entity.setName("bob");
        OrderView dto = modelMapper.map(entity, OrderView.class);
        assertEquals(Long.valueOf(7L), dto.getId());
        assertEquals("bob", dto.getName());
    }
}
