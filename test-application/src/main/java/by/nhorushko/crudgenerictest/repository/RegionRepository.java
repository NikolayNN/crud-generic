package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<RegionEntity, Long> {
}
