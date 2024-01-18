package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.domain.dto.Driver;
import by.nhorushko.crudgenerictest.domain.entity.DriverEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DriverServiceTest {

    @Autowired
    DriverService service;
    @Autowired
    EntityManager entityManager;

    @Test
    public void save() {
        Driver driver = new Driver(null, "Dominic Torretto");
        service.save(1L, driver);
    }

    @Test
    @Sql(statements = "INSERT INTO driver (id, name, user_id) VALUES (33, 'Иваныч',1)")
    public void update() {
        Driver driver = new Driver(33L, "Dominic Torretto");
        service.update(driver);

        entityManager.flush();
        var actual = entityManager.find(DriverEntity.class, 33L);
        entityManager.refresh(actual);
        assertEquals(1L, actual.getUser().getId());
    }
}
