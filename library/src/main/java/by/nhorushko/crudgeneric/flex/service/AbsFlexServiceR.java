package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Only read service
 */
@Transactional
public abstract class AbsFlexServiceR<
        ID,
        ENTITY extends AbstractEntity<ID>,
        READ_DTO extends AbstractDto<ID>,
        REPOSITORY extends JpaRepository<ENTITY, ID>> {

    protected final AbsDtoModelMapper mapper;
    protected final REPOSITORY repository;
    protected final Class<ENTITY> entityClass;
    protected final Class<READ_DTO> readDtoClass;

    @PostConstruct
    private void checkTypeMap() {
        checkTypeMap(entityClass, readDtoClass);
        checkTypeMap(readDtoClass, entityClass);
    }

    protected void checkTypeMap(Class<?> sourceType, Class<?> destinationType) {
        var typeMap = mapper.getModelMapper().getTypeMap(sourceType, destinationType);
        if (typeMap == null) {
            throw new UnsupportedOperationException(String.format("TypeMap for mapping %s -> %s is not exists", sourceType.getSimpleName(), destinationType.getSimpleName()));
        }
    }

    public AbsFlexServiceR(AbsDtoModelMapper mapper,
                           REPOSITORY repository,
                           Class<ENTITY> entityClass,
                           Class<READ_DTO> readDtoClass) {
        this.mapper = mapper;
        this.repository = repository;
        this.entityClass = entityClass;
        this.readDtoClass = readDtoClass;
    }

    public Optional<READ_DTO> getByIdOptional(ID id) {
        return repository.findById(id)
                .map(this::mapReadDto);
    }

    public List<READ_DTO> getById(Collection<ID> ids) {
        List<ENTITY> entities = repository.findAllById(ids);
        return mapAllReadDto(entities);
    }

    public READ_DTO getById(ID id) {
        return getByIdOptional(id)
                .orElseThrow(() -> new AppNotFoundException(format("Entity id: %s was not found", id)));
    }

    public boolean isExist(ID id) {
        return repository.existsById(id);
    }

    protected READ_DTO mapReadDto(ENTITY entity) {
        return this.mapper.map(entity, readDtoClass);
    }

    protected List<READ_DTO> mapAllReadDto(Collection<ENTITY> entities) {
        return this.mapper.mapAll(entities, readDtoClass);
    }
}
