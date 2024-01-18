package by.nhorushko.crudgenerictest.util;

import by.nhorushko.crudgeneric.util.FieldCopyUtil;
import by.nhorushko.crudgenerictest.MockBPartialName;
import by.nhorushko.crudgenerictest.domain.dto.MockBDto;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldCopyUtilTest {

    @Test
    void name() {
        MockBPartialName source = new MockBPartialName("updated");
        MockBDto target = new MockBDto(1l, "name", null);
        FieldCopyUtil.copy(source, target, Set.of());

        assertEquals("updated", target.getName());
    }
}
