package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.v2.service.AbsServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import by.nhorushko.crudgenerictest.mapper.RegionMapper;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import org.springframework.stereotype.Service;

@Service
public class RegionServiceCRUD extends AbsServiceCRUD<Long, RegionEntity, RegionDto, RegionRepository> {
    public RegionServiceCRUD(RegionMapper mapper, RegionRepository repository) {
        super(mapper, repository);
    }
}
