package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperBase;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * Forces eager initialization of crud-generic mapper beans when the consumer
 * application runs with {@code spring.main.lazy-initialization=true}.
 * <p>
 * crud-generic mapper beans (subclasses of {@link AbsMapperBase}, {@link AbsMapBasic},
 * the deprecated {@link AbstractMapper}, and any {@link RegisterableMapper})
 * register {@code TypeMap} and {@code Converter} entries in the shared
 * {@link org.modelmapper.ModelMapper} from their constructors. Under global
 * lazy-init, these beans are not instantiated until somebody injects them by
 * type, which means a direct {@code modelMapper.map(entity, ImmutableDto.class)}
 * call from unrelated code (e.g. event listeners or audit trail) finds no
 * registered converter and falls back to reflective instantiation of the
 * destination type. For Lombok {@code @Value} DTOs without a no-arg constructor
 * this surfaces as a {@code NoSuchMethodException}.
 * </p>
 * <p>
 * This processor walks the {@link BeanDefinitionRegistry} and clears
 * {@code lazyInit} on every bean whose resolved type is assignable to one of the
 * known mapper roots. Behavior is gated by
 * {@link AbsCrudCustomizer#isEagerTypeMapRegistration()} (default {@code true}).
 * In eager-init contexts the {@code setLazyInit(false)} call is a no-op.
 * </p>
 */
public class AbsMapperEagerInitPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // No-op: we only need a hook in postProcessBeanFactory(), where we have the
        // ConfigurableListableBeanFactory and can resolve bean types via getType().
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        AbsCrudCustomizer customizer = beanFactory
                .getBeanProvider(AbsCrudCustomizer.class)
                .getIfAvailable(() -> AbsCrudCustomizer.builder().build());
        if (!customizer.isEagerTypeMapRegistration()) {
            return;
        }
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = resolveBeanType(beanFactory, beanName);
            if (beanType != null && isMapperType(beanType)) {
                beanFactory.getBeanDefinition(beanName).setLazyInit(false);
            }
        }
    }

    private static Class<?> resolveBeanType(ConfigurableListableBeanFactory bf, String beanName) {
        try {
            return bf.getType(beanName, false);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    private static boolean isMapperType(Class<?> clazz) {
        return AbsMapperBase.class.isAssignableFrom(clazz)
            || AbsMapBasic.class.isAssignableFrom(clazz)
            || AbstractMapper.class.isAssignableFrom(clazz)
            || RegisterableMapper.class.isAssignableFrom(clazz);
    }
}
