package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.RegionCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import org.springframework.stereotype.Service;

@Service
public class RegionServiceCRUD extends AbsFlexServiceCRUD<Long, RegionEntity, RegionDto, RegionUpdateDto, RegionCreateDto, RegionRepository> {
    public RegionServiceCRUD(AbsModelMapper mapper, RegionRepository repository) {
        super(mapper, repository, RegionEntity.class, RegionDto.class, RegionUpdateDto.class, RegionCreateDto.class);
    }
}
