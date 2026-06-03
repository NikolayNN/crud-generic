package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
}
