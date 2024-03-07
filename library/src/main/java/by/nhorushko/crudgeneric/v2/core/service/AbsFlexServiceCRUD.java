package by.nhorushko.crudgeneric.v2.core.service;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceRUD;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public abstract class AbsFlexServiceCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbstractDto<ENTITY_ID>,
        CREATE_DTO extends AbstractDto<ENTITY_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>

        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, AbsMapperEntityDto<ENTITY, READ_DTO>, UPDATE_DTO, REPOSITORY> {

    public AbsFlexServiceCRUD(AbsMapperEntityDto<ENTITY, READ_DTO> mapper, REPOSITORY repository, Class<ENTITY> entityClass) {
        super(mapper, repository, entityClass);
    }

    public READ_DTO save(CREATE_DTO dto) {
        ENTITY entity = repository.save(mapper.map(dto, entityClass));
        return mapper.toDto(entity);
    }

    public List<READ_DTO> saveAll(Collection<CREATE_DTO> dto) {
        List<ENTITY> entities = repository.saveAll(mapper.mapAll(dto, entityClass));
        return mapper.toDtos(entities);
    }
}
