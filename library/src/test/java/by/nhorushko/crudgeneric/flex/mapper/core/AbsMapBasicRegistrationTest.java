package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.config.AbsGenericCrudConfiguration;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Registering a mapper must invoke customizeTypeMap exactly once: a second
 * invocation silently re-applies customisations, which breaks additive
 * configuration such as typeMap.addMappings().
 */
public class AbsMapBasicRegistrationTest {

    @Test
    public void customizeTypeMapRunsExactlyOncePerRegistration() {
        ModelMapper modelMapper = new AbsGenericCrudConfiguration().absGenericCrudModelMapper();
        AbsModelMapper mapper = new AbsModelMapper(modelMapper, null);
        AtomicInteger calls = new AtomicInteger();

        new AbsMapBasic<Source, Target>(mapper, Source.class, Target.class) {
            @Override
            protected void customizeTypeMap(TypeMap<Source, Target> typeMap) {
                calls.incrementAndGet();
            }
        };

        assertEquals(1, calls.get());
    }

    public static class Source {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Target {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
