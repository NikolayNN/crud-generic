package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class AbsGenericCrudConfigurationTest {

    /** Spring passes empty lists into the @Bean method when an application declares no mapper at all. */
    @Test
    public void contextWithoutAnyMapperStarts() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(JpaStub.class, AbsGenericCrudConfiguration.class)) {
            MapperRegistry registry = context.getBean(MapperRegistry.class);

            assertFalse(registry.findMapper(Item.class, ItemDto.class).isPresent());
            assertNotNull(context.getBean(AbsMapper.class));
        }
    }

    @Test
    public void declaresOnlyTheRegistryAndTheFacade() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(JpaStub.class, AbsGenericCrudConfiguration.class)) {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            List<String> declared = Arrays.stream(beanFactory.getBeanDefinitionNames())
                    .filter(name -> "absGenericCrudConfiguration".equals(
                            beanFactory.getBeanDefinition(name).getFactoryBeanName()))
                    .sorted()
                    .toList();

            // Only these two come from the configuration, so no mapping-library bean is left in it.
            // The old library's bean names are not spelled out: the spec's verification grep must stay empty.
            assertEquals(List.of("absMapper", "mapperRegistry"), declared);
            for (String removed : List.of("crudAbstractGenericMappingChecker", "absMapperEagerInitPostProcessor")) {
                assertFalse(removed, context.containsBean(removed));
            }
        }
    }

    /**
     * A mapper bean injects AbsMapper while the registry injects every mapper bean: startup must
     * not fail with BeanCurrentlyInCreationException.
     */
    @Test
    public void mapperBeansInjectingTheFacadeDoNotFormACycle() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                JpaStub.class, AbsGenericCrudConfiguration.class, ItemMappers.class)) {
            AbsMapper mapper = context.getBean(AbsMapper.class);

            assertEquals("a", mapper.map(new Item(1L, "a"), ItemDto.class).getName());
        }
    }

    /**
     * Under global lazy init the registry — and with it every mapper bean — is still built at
     * startup, so a broken registration fails the start, not the first request.
     */
    @Test
    public void registryIsBuiltAtStartupUnderLazyInit() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(JpaStub.class, AbsGenericCrudConfiguration.class, ItemMappers.class);
            context.addBeanFactoryPostProcessor(springBootLazyInit());
            context.refresh();

            assertTrue(context.getBeanFactory().containsSingleton("mapperRegistry"));
            assertTrue(context.getBeanFactory().containsSingleton("itemToDto"));
        }
    }

    @Test
    public void duplicatePairFailsStartupUnderLazyInit() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(JpaStub.class, AbsGenericCrudConfiguration.class, DuplicateItemMappers.class);
        context.addBeanFactoryPostProcessor(springBootLazyInit());

        BeanCreationException e = assertThrows(BeanCreationException.class, context::refresh);

        String message = e.getMostSpecificCause().getMessage();
        assertTrue(message, message.contains("bean 'firstItemToDto'"));
        assertTrue(message, message.contains("bean 'secondItemToDto'"));
    }

    /** Two Mapper.of beans are two anonymous Mapper$1 instances: the error must name the beans. */
    @Test
    public void duplicatePairFailsStartupNamingBothBeans() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(JpaStub.class, AbsGenericCrudConfiguration.class, DuplicateItemMappers.class);

        BeanCreationException e = assertThrows(BeanCreationException.class, context::refresh);

        String message = e.getMostSpecificCause().getMessage();
        assertTrue(message, message.startsWith("Duplicate Mapper for " + Item.class.getName()));
        assertTrue(message, message.contains("bean 'firstItemToDto'"));
        assertTrue(message, message.contains("bean 'secondItemToDto'"));
    }

    /**
     * What {@code spring.main.lazy-initialization=true} does in Spring Boot: every bean becomes lazy
     * unless its lazy flag was set explicitly.
     */
    private static BeanFactoryPostProcessor springBootLazyInit() {
        return beanFactory -> {
            for (String name : beanFactory.getBeanDefinitionNames()) {
                if (beanFactory.getBeanDefinition(name) instanceof AbstractBeanDefinition definition
                        && definition.getLazyInit() == null) {
                    definition.setLazyInit(true);
                }
            }
        };
    }

    @Configuration
    public static class JpaStub {
        @Bean
        public EntityManager entityManager() {
            return mock(EntityManager.class);
        }
    }

    @Configuration
    public static class DuplicateItemMappers {
        @Bean
        public Mapper<Item, ItemDto> firstItemToDto() {
            return Mapper.of(Item.class, ItemDto.class, item -> new ItemDto(item.getId(), item.getName()));
        }

        @Bean
        public Mapper<Item, ItemDto> secondItemToDto() {
            return Mapper.of(Item.class, ItemDto.class, item -> new ItemDto(item.getId(), item.getName()));
        }
    }

    @Configuration
    public static class ItemMappers {
        @Bean
        public ItemToDto itemToDto(AbsMapper mapper) {
            return new ItemToDto(mapper);
        }
    }

    public static class ItemToDto extends AbsMapEntityToDto<Item, ItemDto> {
        public ItemToDto(AbsMapper mapper) {
            super(mapper, Item.class, ItemDto.class);
        }

        @Override
        protected ItemDto create(Item from) {
            return new ItemDto(from.getId(), from.getName());
        }
    }

    public static class Item implements AbstractEntity<Long> {
        private Long id;
        private final String name;

        public Item(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }
    }

    public static class ItemDto implements AbstractDto<Long> {
        private final Long id;
        private final String name;

        public ItemDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
