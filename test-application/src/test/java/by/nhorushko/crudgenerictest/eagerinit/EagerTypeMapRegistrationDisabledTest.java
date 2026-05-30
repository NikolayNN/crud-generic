package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgeneric.flex.config.AbsCrudCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.datasource.url=jdbc:h2:mem:lazytestdb;NON_KEYWORDS=USER"
})
class EagerTypeMapRegistrationDisabledTest {

    @TestConfiguration
    static class Config {
        @Bean
        AbsCrudCustomizer absCrudCustomizer() {
            return AbsCrudCustomizer.builder()
                    .eagerTypeMapRegistration(false)
                    .typeMapCheckerEnabled(false)
                    .build();
        }
    }

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void mockAImmutableMapper_beanDefinitionRemainsLazy() {
        boolean lazy = applicationContext.getBeanFactory()
                .getBeanDefinition("mockAImmutableMapper")
                .isLazyInit();
        assertTrue(lazy, "mockAImmutableMapper should stay lazy when eagerTypeMapRegistration is false");
    }
}
