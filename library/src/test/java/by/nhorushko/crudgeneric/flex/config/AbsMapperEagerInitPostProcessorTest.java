package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.composite.AbsFlexMapConfigAbstract;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperBase;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbsMapperEagerInitPostProcessorTest {

    @Test
    public void postProcessBeanFactory_flagOn_setsLazyInitFalseForMapperSubtype() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubMapperBaseBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("stubMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("stubMapper").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alreadyEagerMapperStaysEager() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubMapperBaseBean.class);
        mapperBd.setLazyInit(false);
        bf.registerBeanDefinition("eagerMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("eagerMapper").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alsoFlipsAbsMapBasicSubtypes() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubAbsMapBasicBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("absMapBasicBean", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("absMapBasicBean").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alsoFlipsAbstractMapperSubtypes() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubAbstractMapperBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("legacyMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("legacyMapper").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alsoFlipsAbsFlexMapConfigSubtypes() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubFlexMapConfigBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("flexMapConfig", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("flexMapConfig").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_alsoFlipsRegisterableMapperImplementors() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition mapperBd = new RootBeanDefinition(StubRegisterableBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("registerable", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("registerable").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOn_leavesNonMapperLazyAlone() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(true);

        BeanDefinition unrelatedBd = new RootBeanDefinition(StubUnrelatedBean.class);
        unrelatedBd.setLazyInit(true);
        bf.registerBeanDefinition("unrelated", unrelatedBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertTrue(bf.getBeanDefinition("unrelated").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_flagOff_leavesMapperLazyAlone() {
        DefaultListableBeanFactory bf = newFactoryWithCustomizer(false);

        BeanDefinition mapperBd = new RootBeanDefinition(StubMapperBaseBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("stubMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertTrue(bf.getBeanDefinition("stubMapper").isLazyInit());
    }

    @Test
    public void postProcessBeanFactory_noCustomizerBean_defaultsToFlagOn() {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

        BeanDefinition mapperBd = new RootBeanDefinition(StubMapperBaseBean.class);
        mapperBd.setLazyInit(true);
        bf.registerBeanDefinition("stubMapper", mapperBd);

        new AbsMapperEagerInitPostProcessor().postProcessBeanFactory(bf);

        assertFalse(bf.getBeanDefinition("stubMapper").isLazyInit());
    }

    private static DefaultListableBeanFactory newFactoryWithCustomizer(boolean eager) {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .eagerTypeMapRegistration(eager)
                .build();
        // Register the customizer as a singleton so getBeanProvider().getIfAvailable() finds it.
        bf.registerSingleton("absCrudCustomizer", customizer);
        return bf;
    }

    // --- stubs --- //

    public static class StubMapperBaseBean extends AbsMapperBase<Object, Object> {
        public StubMapperBaseBean() {
            super(null, Object.class, Object.class);
        }
    }

    public static class StubAbsMapBasicBean extends AbsMapBasic<Object, Object> {
        // null AbsModelMapper is intentional and safe: BDRPP only inspects bean
        // definitions via getType(beanName, false) and never instantiates the bean,
        // so AbsMapBasic's constructor (which would dereference the null mapper in
        // register()) never runs. Do not change this without rethinking the test design.
        public StubAbsMapBasicBean() {
            super(null, Object.class, Object.class);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class StubAbstractMapperBean extends AbstractMapper {
        public StubAbstractMapperBean() {
            super(by.nhorushko.crudgeneric.domain.AbstractEntity.class,
                  by.nhorushko.crudgeneric.domain.AbstractDto.class,
                  null);
        }
    }

    public static class StubRegisterableBean implements RegisterableMapper {
        @Override
        public void register() { /* no-op */ }
    }

    // BDRPP only inspects bean definitions via getType(beanName, false) and never
    // instantiates the bean, so the parent ctor (which would dereference the null
    // AbsModelMapper while creating the inner AbsMapBasic side-effects) never runs.
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class StubFlexMapConfigBean extends AbsFlexMapConfigAbstract {
        public StubFlexMapConfigBean() {
            super(null, Object.class, Object.class, Object.class, Object.class);
        }
        @Override
        protected AbsMapBasic mapperCreateDtoToEntity(AbsModelMapper m, Class a, Class b) { return null; }
        @Override
        protected AbsMapBasic mapperUpdateDtoToEntity(AbsModelMapper m, Class a, Class b) { return null; }
        @Override
        protected AbsMapBasic mapperReadDtoToEntity(AbsModelMapper m, Class a, Class b) { return null; }
        @Override
        protected AbsMapBasic mapperEntityToReadDto(AbsModelMapper m, Class a, Class b) { return null; }
    }

    public static class StubUnrelatedBean { }
}
