package by.nhorushko.crudgeneric.flex.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables the Generic CRUD Framework in a Spring Boot application.
 * <p>
 * When placed on a Spring Boot application's main class or any configuration class,
 * this annotation imports the {@link AbsGenericCrudConfiguration} class into the
 * Spring context. This action configures the necessary beans for ModelMapper, sets up
 * custom DTO to entity mappings, and initializes any additional configurations required
 * by the Generic CRUD Framework.
 * </p>
 * <p>
 * Usage of this annotation allows for quick integration of the framework, enabling
 * developers to leverage simplified DTO to entity mappings, extended CRUD operations,
 * and predefined hooks for custom business logic with minimal manual configuration.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAbsGenericCrud
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 *
 * @see AbsGenericCrudConfiguration for details on the specific beans and configurations provided by the framework.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(AbsGenericCrudConfiguration.class)
public @interface EnableAbsGenericCrud {
}

