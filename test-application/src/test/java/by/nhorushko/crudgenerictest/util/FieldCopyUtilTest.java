package by.nhorushko.crudgenerictest.util;

import by.nhorushko.crudgeneric.flex.util.FieldCopyUtil;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldCopyUtilTest {

    @Test
    void copiesMatchingFieldsAndSkipsIgnored() {
        Source source = new Source(7L, "name-1", "source-secret");
        Target target = new Target();

        FieldCopyUtil.copy(source, target, Set.of("secret"));

        assertEquals(7L, target.id);
        assertEquals("name-1", target.name);
        assertEquals("target-secret", target.secret);
    }

    private static final class Source {
        private final Long id;
        private final String name;
        private final String secret;

        private Source(Long id, String name, String secret) {
            this.id = id;
            this.name = name;
            this.secret = secret;
        }
    }

    private static final class Target {
        private Long id;
        private String name;
        private String secret = "target-secret";
    }
}
