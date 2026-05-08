package by.nhorushko.crudgeneric.flex.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbsCrudCustomizerEagerFlagTest {

    @Test
    public void builder_defaultEagerTypeMapRegistration_isTrue() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder().build();
        assertTrue(customizer.isEagerTypeMapRegistration());
    }

    @Test
    public void builder_eagerTypeMapRegistrationFalse_returnsFalse() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .eagerTypeMapRegistration(false)
                .build();
        assertFalse(customizer.isEagerTypeMapRegistration());
    }

    @Test
    public void builder_typeMapCheckerStaysIndependent() {
        AbsCrudCustomizer customizer = AbsCrudCustomizer.builder()
                .eagerTypeMapRegistration(false)
                .build();
        assertTrue(customizer.isTypeMapCheckerEnabled());
    }
}
