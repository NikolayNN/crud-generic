package by.nhorushko.crudgeneric.v2.core.service;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityExtDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceRUD;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 *
 * Read Update Delete Create Service
 */
public abstract class AbsFlexServiceExtCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbstractDto<ENTITY_ID>,
        CREATE_DTO extends AbstractDto<ENTITY_ID>,
        EXT_ID,
        EXT extends AbstractEntity<EXT_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, AbsMapperEntityDto<ENTITY, READ_DTO>, UPDATE_DTO, REPOSITORY> {

    private final AbsMapperEntityExtDto<ENTITY, CREATE_DTO, EXT_ID, EXT> extCreateMapper;

    public AbsFlexServiceExtCRUD(AbsMapperEntityDto<ENTITY, READ_DTO> readMapper,
                                 REPOSITORY repository,
                                 Class<ENTITY> entityClass,
                                 AbsMapperEntityExtDto<ENTITY, CREATE_DTO, EXT_ID, EXT> extCreateMapper) {
        super(readMapper, repository, entityClass);
        this.extCreateMapper = extCreateMapper;
    }

    public READ_DTO save(EXT_ID relationId, CREATE_DTO dto) {
        if (dto.isNew()) {
            ENTITY entity = repository.save(extCreateMapper.toEntity(relationId, dto));
            return mapper.toDto(entity);
        }
        throw new IllegalArgumentException(wrongIdMessage(dto.getId()));
    }

    public List<READ_DTO> saveAll(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
        List<ENTITY> entities = extCreateMapper.toEntities(relationId, dtos);
        entities.forEach(e -> {
            if (!e.isNew()) throw new IllegalArgumentException(wrongIdMessage(e.getId()));
        });
        entities = repository.saveAll(extCreateMapper.toEntities(relationId, dtos));
        return mapper.toDtos(entities);
    }

    private String wrongIdMessage(ENTITY_ID id) {
        return String.format("wrong id: %s to save new entity id should be null", id);
    }
}
