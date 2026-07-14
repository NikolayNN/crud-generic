package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.composite.AbsFlexMapConfigDefault;
import by.nhorushko.crudgenerictest.domain.dto.RegionCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import org.springframework.stereotype.Component;

@Component
public class RegionMapConfig extends AbsFlexMapConfigDefault<RegionCreateDto, RegionUpdateDto, RegionDto, RegionEntity> {

    public RegionMapConfig(AbsModelMapper mapper) {
        super(mapper, RegionCreateDto.class, RegionUpdateDto.class, RegionDto.class, RegionEntity.class);
    }

    @Override
    protected RegionDto createReadDtoFromEntity(AbsModelMapper mapper, RegionEntity entity) {
        return new RegionDto(entity.getId(), entity.getName());
    }
}
