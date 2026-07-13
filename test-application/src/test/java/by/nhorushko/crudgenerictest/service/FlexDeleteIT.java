package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import by.nhorushko.crudgenerictest.repository.OrderLineRepository;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for the flex delete path: deleting an existing entity
 * removes it (with cascaded children), deleting a missing id must fail loudly
 * instead of being a silent no-op.
 */
@SpringBootTest
class FlexDeleteIT {

    @Autowired
    private OrderServiceCRUD service;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderLineRepository lineRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void deleteRemovesOrderAndCascadesToChildren() {
        OrderEntity order = persistedOrder("order", "line-1", "line-2");

        service.delete(order.getId());

        assertThat(orderRepository.findById(order.getId())).isEmpty();
        assertThat(lineRepository.count()).isZero();
    }

    @Test
    void deleteMissingIdThrowsAppNotFound() {
        assertThatThrownBy(() -> service.delete(999_999L))
                .isInstanceOf(AppNotFoundException.class);
    }

    private OrderEntity persistedOrder(String name, String... lineTitles) {
        OrderEntity order = new OrderEntity();
        order.setName(name);
        order.setSecretCode("s3cret");
        for (String title : lineTitles) {
            OrderLineEntity line = new OrderLineEntity();
            line.setTitle(title);
            order.getLines().add(line);
        }
        return orderRepository.save(order);
    }
}
