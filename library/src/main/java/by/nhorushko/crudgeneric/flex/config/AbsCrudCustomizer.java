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
 *
 * <p>Example: opt out of eager mapper-bean initialization under
 * {@code spring.main.lazy-initialization=true}:</p>
 * <pre>
 * &#64;Bean
 * public AbsCrudCustomizer absCrudCustomizer() {
 *     return AbsCrudCustomizer.builder()
 *             .eagerTypeMapRegistration(false)
 *             .build();
 * }
 * </pre>
 *
 * <p>The {@code eagerTypeMapRegistration} flag controls whether crud-generic
 * forces the following bean classes to be eagerly initialized regardless of
 * the global lazy-init setting:</p>
 * <ul>
 *   <li>{@code by.nhorushko.crudgeneric.v2.mapper.AbsMapperBase} and subclasses</li>
 *   <li>{@code by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic} and subclasses</li>
 *   <li>{@code by.nhorushko.crudgeneric.mapper.AbstractMapper} (deprecated) and subclasses</li>
 *   <li>{@code by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper} implementations</li>
 * </ul>
 * <p>These beans register {@code TypeMap} / {@code Converter} entries in the shared
 * {@code ModelMapper} from their constructors, so they must be instantiated before
 * any consumer calls {@code modelMapper.map(...)}.</p>
 */
@Getter
@Builder
public class AbsCrudCustomizer {

    @Builder.Default
    private final boolean typeMapCheckerEnabled = true;

    @Builder.Default
    private final boolean eagerTypeMapRegistration = true;
}
