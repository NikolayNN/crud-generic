package by.nhorushko.crudgenerictest.repository;

import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MeetingRepository extends JpaRepository<MeetingEntity, Long>, JpaSpecificationExecutor<MeetingEntity> {
}
