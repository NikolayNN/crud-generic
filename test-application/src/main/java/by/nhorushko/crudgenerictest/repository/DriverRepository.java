package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<DriverEntity, Long> {
}
