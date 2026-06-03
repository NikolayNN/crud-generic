package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import jakarta.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public final class RegionMapper extends AbsMapperEntityDto<RegionEntity, RegionDto> {
    public RegionMapper(ModelMapper modelMapper, EntityManager entityManager) {
        super(modelMapper, entityManager, RegionEntity.class, RegionDto.class);
    }

    @Override
    protected RegionDto create(RegionEntity from) {
        return new RegionDto(from.getId(), from.getName());
    }
}
