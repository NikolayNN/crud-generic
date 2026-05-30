package by.nhorushko.crudgeneric.flex.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbsCrudCustomizerTest {

    @Test
    public void builder_defaultTypeMapCheckerEnabled_isTrue() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder().build();
        assertTrue(customizer.isTypeMapCheckerEnabled());
    }

    @Test
    public void builder_typeMapCheckerEnabledFalse_returnsFalse() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .typeMapCheckerEnabled(false)
                .build();
        assertFalse(customizer.isTypeMapCheckerEnabled());
    }
}
