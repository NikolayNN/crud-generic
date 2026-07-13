package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
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
 * Regression tests for the flex update path: an UPDATE_DTO deliberately carries
 * only a subset of the entity's fields ({@code secretCode} and {@code lines} are
 * absent), and updating through it must not wipe the fields it does not carry.
 */
@SpringBootTest
class FlexUpdateIT {

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
    void updateAppliesDtoFields() {
        OrderEntity order = persistedOrder("old", "s3cret");

        OrderDto updated = service.update(new OrderUpdateDto(order.getId(), "new"));

        assertThat(updated.getName()).isEqualTo("new");
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("new");
    }

    @Test
    void updatePreservesEntityFieldAbsentFromUpdateDto() {
        OrderEntity order = persistedOrder("old", "s3cret");

        service.update(new OrderUpdateDto(order.getId(), "new"));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getSecretCode())
                .isEqualTo("s3cret");
    }

    @Test
    void updatePreservesChildrenWhenUpdateDtoOmitsThem() {
        OrderEntity order = persistedOrder("old", "s3cret", "line-1", "line-2");
        assertThat(lineRepository.count()).isEqualTo(2L);

        service.update(new OrderUpdateDto(order.getId(), "new"));

        assertThat(lineRepository.count()).isEqualTo(2L);
    }

    @Test
    void updatePartialPreservesFieldsAbsentFromPartial() {
        OrderEntity order = persistedOrder("old", "s3cret", "line-1");

        service.updatePartial(order.getId(), new NamePatch("new"));

        OrderEntity actual = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(actual.getName()).isEqualTo("new");
        assertThat(actual.getSecretCode()).isEqualTo("s3cret");
        assertThat(lineRepository.count()).isEqualTo(1L);
    }

    @Test
    void updateMissingIdThrowsAppNotFound() {
        assertThatThrownBy(() -> service.update(new OrderUpdateDto(999_999L, "x")))
                .isInstanceOf(AppNotFoundException.class);
        assertThat(orderRepository.count()).isZero();
    }

    private OrderEntity persistedOrder(String name, String secretCode, String... lineTitles) {
        OrderEntity order = new OrderEntity();
        order.setName(name);
        order.setSecretCode(secretCode);
        for (String title : lineTitles) {
            OrderLineEntity line = new OrderLineEntity();
            line.setTitle(title);
            order.getLines().add(line);
        }
        return orderRepository.save(order);
    }

    private static final class NamePatch {
        private final String name;

        private NamePatch(String name) {
            this.name = name;
        }
    }
}
