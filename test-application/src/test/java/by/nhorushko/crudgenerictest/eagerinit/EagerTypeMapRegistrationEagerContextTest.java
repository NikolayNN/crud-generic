package by.nhorushko.crudgenerictest.eagerinit;

import by.nhorushko.crudgenerictest.domain.dto.MockAImmutableDto;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class EagerTypeMapRegistrationEagerContextTest {

    @Autowired
    private ModelMapper modelMapper;

    @Test
    void modelMapperMap_immutableDto_succeedsInEagerContext() {
        MockAEntity entity = new MockAEntity(7L, "bob", "desc");

        MockAImmutableDto dto = modelMapper.map(entity, MockAImmutableDto.class);

        assertEquals(Long.valueOf(7L), dto.getId());
        assertEquals("bob", dto.getName());
    }
}
