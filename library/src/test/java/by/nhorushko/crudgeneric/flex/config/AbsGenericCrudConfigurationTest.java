package by.nhorushko.crudgeneric.flex.config;

import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsGenericCrudConfigurationTest {

    private AbsGenericCrudConfiguration configuration;
    private ModelMapper modelMapper;

    @Before
    public void setUp() {
        configuration = new AbsGenericCrudConfiguration();
        modelMapper = mock(ModelMapper.class);
    }

    @Test
    public void crudAbstractGenericMappingChecker_noCustomizer_defaultsToEnabled() {
        ObjectProvider<AbsCrudCustomizer> provider = emptyProvider();

        AbsTypeMapChecker checker = configuration.crudAbstractGenericMappingChecker(
                Collections.emptyList(), modelMapper, provider);

        // start() with no services should not throw and should set running.
        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void crudAbstractGenericMappingChecker_customizerDisabled_skipsValidation() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .typeMapCheckerEnabled(false)
                .build();
        ObjectProvider<AbsCrudCustomizer> provider = providerOf(customizer);

        // Build a service that would fail validation if the checker ran.
        by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR<?, ?, ?, ?> failingService =
                mock(by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR.class);
        when((Class) failingService.getEntityClass()).thenReturn(Object.class);
        when((Class) failingService.getReadDtoClass()).thenReturn(String.class);
        when(modelMapper.getTypeMap(any(), any())).thenReturn(null);

        AbsTypeMapChecker checker = configuration.crudAbstractGenericMappingChecker(
                Collections.singletonList(failingService), modelMapper, provider);

        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void crudAbstractGenericMappingChecker_customizerEnabled_runsValidation() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .typeMapCheckerEnabled(true)
                .build();
        ObjectProvider<AbsCrudCustomizer> provider = providerOf(customizer);

        by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR<?, ?, ?, ?> failingService =
                mock(by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR.class);
        when((Class) failingService.getEntityClass()).thenReturn(Object.class);
        when((Class) failingService.getReadDtoClass()).thenReturn(String.class);
        when(modelMapper.getTypeMap(any(), any())).thenReturn(null);

        AbsTypeMapChecker checker = configuration.crudAbstractGenericMappingChecker(
                Collections.singletonList(failingService), modelMapper, provider);

        try {
            checker.start();
            fail("Expected UnsupportedOperationException because validation should run");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AbsCrudCustomizer> emptyProvider() {
        ObjectProvider<AbsCrudCustomizer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenAnswer(inv -> {
            java.util.function.Supplier<AbsCrudCustomizer> supplier = inv.getArgument(0);
            return supplier.get();
        });
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AbsCrudCustomizer> providerOf(AbsCrudCustomizer customizer) {
        ObjectProvider<AbsCrudCustomizer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(customizer);
        return provider;
    }
}
