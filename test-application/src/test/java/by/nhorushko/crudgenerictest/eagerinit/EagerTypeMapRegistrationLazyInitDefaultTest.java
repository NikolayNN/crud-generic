package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgenerictest.domain.dto.MockAImmutableDto;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class EagerTypeMapRegistrationLazyInitDefaultTest {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void modelMapperMap_immutableDto_succeedsUnderLazyInitWithDefaults() {
        MockAEntity entity = new MockAEntity(42L, "alice", "desc");

        MockAImmutableDto dto = modelMapper.map(entity, MockAImmutableDto.class);

        assertEquals(Long.valueOf(42L), dto.getId());
        assertEquals("alice", dto.getName());
    }

    @Test
    void mockAImmutableMapper_beanDefinitionIsEager() {
        // The post-processor should have flipped this mapper's BeanDefinition out of lazy.
        boolean lazy = applicationContext.getBeanFactory()
                .getBeanDefinition("mockAImmutableMapper")
                .isLazyInit();
        assertFalse(lazy, "mockAImmutableMapper should be eager because eagerTypeMapRegistration defaults to true");
    }
}
