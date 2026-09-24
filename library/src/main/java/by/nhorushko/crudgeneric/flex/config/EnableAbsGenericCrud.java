package by.nhorushko.crudgeneric.flex.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables the Generic CRUD Framework in a Spring Boot application.
 * <p>
 * Placed on the application's main class or any configuration class, it imports
 * {@link AbsGenericCrudConfiguration}, which registers the
 * {@link by.nhorushko.crudgeneric.flex.mapper.MapperRegistry} built from every mapper bean and the
 * {@link by.nhorushko.crudgeneric.flex.AbsMapper} facade the services map through.
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
 * @see AbsGenericCrudConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(AbsGenericCrudConfiguration.class)
public @interface EnableAbsGenericCrud {
}

