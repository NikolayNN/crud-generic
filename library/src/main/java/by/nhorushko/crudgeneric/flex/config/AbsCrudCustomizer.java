package by.nhorushko.crudgeneric.flex.config;

import lombok.Builder;
import lombok.Getter;

/**
 * Customizes the runtime behavior of the Generic CRUD framework.
 * <p>
 * Register a single {@link AbsCrudCustomizer} bean in the application's Spring context
 * to override the framework defaults. When no bean is provided, the framework uses
 * defaults equivalent to {@code AbsCrudCustomizer.builder().build()}.
 * </p>
 *
 * <p>Example: disable startup-time ModelMapper validation:</p>
 * <pre>
 * &#64;Bean
 * public AbsCrudCustomizer absCrudCustomizer() {
 *     return AbsCrudCustomizer.builder()
 *             .typeMapCheckerEnabled(false)
 *             .build();
 * }
 * </pre>
 */
@Getter
@Builder
public class AbsCrudCustomizer {

    @Builder.Default
    private final boolean typeMapCheckerEnabled = true;
}
