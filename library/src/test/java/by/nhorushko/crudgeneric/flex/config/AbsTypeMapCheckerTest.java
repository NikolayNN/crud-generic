package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsTypeMapCheckerTest {

    private ModelMapper modelMapper;
    private AbsFlexServiceR<?, ?, ?, ?> serviceWithMissingMapping;

    @Before
    public void setUp() {
        modelMapper = mock(ModelMapper.class);
        // No type maps configured -> getTypeMap returns null for any pair.
        when(modelMapper.getTypeMap(Object.class, String.class)).thenReturn(null);

        serviceWithMissingMapping = mock(AbsFlexServiceR.class);
        when((Class) serviceWithMissingMapping.getEntityClass()).thenReturn(Object.class);
        when((Class) serviceWithMissingMapping.getReadDtoClass()).thenReturn(String.class);
    }

    @Test
    public void start_enabledTrue_throwsWhenMappingMissing() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                List.of(serviceWithMissingMapping), modelMapper, true);
        try {
            checker.start();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void start_enabledFalse_doesNotThrowWhenMappingMissing() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                List.of(serviceWithMissingMapping), modelMapper, false);
        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    public void start_enabledFalse_emptyServices_isRunning() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                Collections.emptyList(), modelMapper, false);
        checker.start();
        assertTrue(checker.isRunning());
    }

    @Test
    public void legacyConstructor_defaultsToEnabled() {
        AbsTypeMapChecker checker = new AbsTypeMapChecker(
                List.of(serviceWithMissingMapping), modelMapper);
        try {
            checker.start();
            fail("Expected legacy constructor to default to enabled=true and throw");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }
}
