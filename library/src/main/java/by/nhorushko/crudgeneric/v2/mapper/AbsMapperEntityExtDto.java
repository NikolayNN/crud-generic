package by.nhorushko.crudgeneric.v2.mapper;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbsMapperEntityExtDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>, EXT_ID,
        EXT extends AbstractEntity<EXT_ID>>
        extends AbsMapperEntityDto<ENTITY, DTO> {
    protected final EntityManager entityManager;
    protected final Class<EXT> extClass;

    public AbsMapperEntityExtDto(ModelMapper modelMapper, Class<ENTITY> entityClass, Class<DTO> dtoClass,
                                 EntityManager entityManager, Class<EXT> extClass) {
        super(modelMapper, entityClass, dtoClass);
        this.entityManager = entityManager;
        this.extClass = extClass;
    }

    public ENTITY toEntity(EXT_ID relationId, DTO dto) {
        if (relationId == null || dto == null) {
            return null;
        }
        ENTITY entity = map(dto, entityClass);
        setRelation(relationId, entity);
        return entity;
    }

    public List<ENTITY> toEntities(EXT_ID relationId, Collection<DTO> dtos) {
        return dtos.stream()
                .map(d -> toEntity(relationId, d))
                .collect(Collectors.toList());
    }

    protected abstract void setRelation(EXT ext, ENTITY destination);

    private void setRelation(EXT_ID relationId, ENTITY destination) {
        EXT ext = entityManager.getReference(extClass, relationId);
        setRelation(ext, destination);
    }
}
