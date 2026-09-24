package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderNamePatch;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The write paths of AbsFlexServiceRUD on the real Spring/Hibernate stack: patch(id, body) goes
 * through the Updater declared in OrderMapConfig.patches(), rename goes through changeEntity, a body
 * without an Updater fails loudly, and the overridden loadForUpdate / saveUpdated seams sit on all
 * three paths. A dedicated H2 url forks the cached context so the extra service bean stays local.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:patchdb;NON_KEYWORDS=USER")
class FlexPatchIT {

    @Autowired
    private RecordingOrderService service;
    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
        service.events().clear();
    }

    @Test
    void patchGoesThroughRegisteredUpdaterAndUpdateHooks() {
        OrderEntity order = persistedOrder("old");

        OrderDto patched = service.patch(order.getId(), new OrderNamePatch("new"));

        assertThat(patched.getName()).isEqualTo("new");
        OrderEntity actual = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(actual.getName()).isEqualTo("new");
        assertThat(actual.getSecretCode()).isEqualTo("s3cret");
        assertThat(service.events()).containsExactly("beforePatch", "load", "save", "afterUpdate:new");
    }

    @Test
    void renameGoesThroughChangeEntityWithoutBeforeHooks() {
        OrderEntity order = persistedOrder("old");

        OrderDto renamed = service.rename(order.getId(), "renamed");

        assertThat(renamed.getName()).isEqualTo("renamed");
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("renamed");
        assertThat(service.events()).containsExactly("load", "save", "afterUpdate:renamed");
    }

    @Test
    void patchBodyWithoutUpdaterFailsWithMappingNotFound() {
        OrderEntity order = persistedOrder("old");

        assertThatThrownBy(() -> service.patch(order.getId(), new UnregisteredPatch("x")))
                .isInstanceOf(MappingNotFoundException.class)
                .hasMessageContaining(UnregisteredPatch.class.getName());
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("old");
        assertThat(service.events()).doesNotContain("save");
    }

    @Test
    void overriddenSeamsRunOnAllThreeWritePaths() {
        OrderEntity order = persistedOrder("old");

        service.update(new OrderUpdateDto(order.getId(), "via-update"));
        service.patch(order.getId(), new OrderNamePatch("via-patch"));
        service.rename(order.getId(), "via-change");

        assertThat(service.events()).filteredOn("load"::equals).hasSize(3);
        assertThat(service.events()).filteredOn("save"::equals).hasSize(3);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getName()).isEqualTo("via-change");
    }

    private OrderEntity persistedOrder(String name) {
        OrderEntity order = new OrderEntity();
        order.setName(name);
        order.setSecretCode("s3cret");
        return orderRepository.save(order);
    }

    @TestConfiguration
    static class Config {
        @Bean
        RecordingOrderService recordingOrderService(AbsMapper mapper, OrderRepository repository) {
            return new RecordingOrderService(mapper, repository);
        }
    }

    /**
     * The bean is a CGLIB proxy (class-level @Transactional), and a proxy's own fields are never
     * initialised: read the events through the public events() method, never the field.
     */
    static class RecordingOrderService extends OrderServiceCRUD {
        private final List<String> events = new ArrayList<>();

        RecordingOrderService(AbsMapper mapper, OrderRepository repository) {
            super(mapper, repository);
        }

        public List<String> events() {
            return events;
        }

        @Override
        protected void beforeUpdateHook(OrderUpdateDto dto) {
            events.add("beforeUpdate");
        }

        @Override
        protected void beforePatchHook(Long id, Object body) {
            events.add("beforePatch");
        }

        @Override
        protected OrderEntity loadForUpdate(Long id) {
            events.add("load");
            return super.loadForUpdate(id);
        }

        @Override
        protected OrderEntity saveUpdated(OrderEntity entity) {
            events.add("save");
            return super.saveUpdated(entity);
        }

        @Override
        protected void afterUpdateHook(OrderDto dto) {
            events.add("afterUpdate:" + dto.getName());
        }
    }

    record UnregisteredPatch(String name) {
    }
}
