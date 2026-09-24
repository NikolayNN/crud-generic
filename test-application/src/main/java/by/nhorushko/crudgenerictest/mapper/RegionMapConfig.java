package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsFlexMapConfig;
import by.nhorushko.crudgenerictest.domain.dto.RegionCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import org.springframework.stereotype.Component;

@Component
public class RegionMapConfig extends AbsFlexMapConfig<RegionCreateDto, RegionUpdateDto, RegionDto, RegionEntity> {

    public RegionMapConfig(AbsMapper mapper) {
        super(mapper, RegionCreateDto.class, RegionUpdateDto.class, RegionDto.class, RegionEntity.class);
    }

    /** Region ids are assigned by the client and save() is an upsert, so the id must be copied. */
    @Override
    protected RegionEntity toEntity(RegionCreateDto dto) {
        RegionEntity entity = new RegionEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        return entity;
    }

    @Override
    protected void updateEntity(RegionUpdateDto dto, RegionEntity entity) {
        entity.setName(dto.getName());
    }

    @Override
    protected RegionDto toReadDto(RegionEntity entity) {
        return new RegionDto(entity.getId(), entity.getName());
    }
}
