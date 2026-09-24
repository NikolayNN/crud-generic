package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgenerictest.domain.dto.OrderCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderLineDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import by.nhorushko.crudgenerictest.repository.OrderLineRepository;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flex twin of {@code SaveV1CascadeIT}: nested child DTOs carrying the sentinel
 * id 0 must be normalised to null by the flex mapper stack so cascaded children
 * insert cleanly.
 */
@SpringBootTest
class FlexSaveCascadeIT {

    @Autowired
    private OrderServiceCRUD service;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderLineRepository lineRepository;
    @Autowired
    private AbsMapper mapper;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void createWithSentinelZeroIdChildInsertsChild() {
        OrderDto saved = service.save(new OrderCreateDto("order", List.of(new OrderLineDto(0L, "child"))));

        assertThat(saved.getId()).isNotNull();
        assertThat(lineRepository.count()).isEqualTo(1L);
        OrderLineEntity line = lineRepository.findAll().get(0);
        assertThat(line.getId()).isPositive();
        assertThat(line.getTitle()).isEqualTo("child");
    }

    /**
     * mapAll(null) is null by the facade's contract, so the config must choose explicitly what a
     * null list means: here "no children", keeping the entity's initialised collection.
     */
    @Test
    void createWithNullLinesKeepsAnEmptyCollection() {
        OrderEntity entity = mapper.map(new OrderCreateDto("order", null), OrderEntity.class);

        assertThat(entity.getLines()).isNotNull().isEmpty();
    }
}
