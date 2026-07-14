package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgenerictest.domain.dto.OrderView;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.datasource.url=jdbc:h2:mem:lazytestdb;NON_KEYWORDS=USER"
})
class EagerTypeMapRegistrationLazyInitDefaultTest {

    @Autowired
    @Qualifier("modelMapper")
    private ModelMapper modelMapper;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void modelMapperMap_immutableDto_succeedsUnderLazyInitWithDefaults() {
        OrderEntity entity = new OrderEntity();
        entity.setId(42L);
        entity.setName("alice");
        OrderView dto = modelMapper.map(entity, OrderView.class);
        assertEquals(Long.valueOf(42L), dto.getId());
        assertEquals("alice", dto.getName());
    }

    @Test
    void orderViewMapper_beanDefinitionIsEager() {
        boolean lazy = applicationContext.getBeanFactory()
                .getBeanDefinition("orderViewMapper").isLazyInit();
        assertFalse(lazy, "orderViewMapper should be eager because eagerTypeMapRegistration defaults to true");
    }
}
