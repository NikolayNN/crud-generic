package by.nhorushko.crudgeneric.service;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgeneric.mapper.DtoMappers;
import by.nhorushko.crudgeneric.mapper.Mapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
@Transactional
public abstract class ImmutableGenericService<
        DTO extends AbstractDto,
        ENTITY extends AbstractEntity,
        REPOSITORY extends JpaRepository<ENTITY, Long>,
        MAPPER extends AbstractMapper<ENTITY, DTO>>
        implements ImmutableGenericServiceI<DTO> {

    protected final REPOSITORY repository;
    protected final MAPPER mapper;
    protected final Class<DTO> dtoClass;
    protected final Class<ENTITY> entityClass;

    public ImmutableGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        this.repository = repository;
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.entityClass = entityClass;
    }

    @Override
    public List<DTO> list() {
        return mapper.toDto(repository.findAll());
    }

    public <DTO_PARTIAL extends AbstractDto> List<DTO_PARTIAL> list(Class<DTO_PARTIAL> dtoClass) {
        return getMapper(dtoClass).toDto(repository.findAll());
    }

    @Override
    public DTO getById(Long id) {
        return mapper.toDto(findEntityById(id));
    }

    public <DTO_PARTIAL extends AbstractDto> DTO_PARTIAL getById(Long id, Class<DTO_PARTIAL> dto_partialClass) {
        return getMapper(dto_partialClass).toDto(findEntityById(id));
    }

    public ENTITY findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> notFoundException(id));
    }

    protected AppNotFoundException notFoundException(Long id) {
        return new AppNotFoundException(String.format("Entity %s id: %s was not found", entityClass, id));
    }

    @Override
    public List<DTO> getById(Collection<Long> ids) {
        return mapper.toDto(repository.findAllById(ids));
    }

    public <DTO_PARTIAL extends AbstractDto> List<DTO_PARTIAL> getById(Collection<Long> ids, Class<DTO_PARTIAL> dto_partialClass) {
        return getMapper(dto_partialClass).toDto(repository.findAllById(ids));
    }

    @Override
    public boolean existById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    protected <DTO_PARTIAL extends AbstractDto> Mapper<ENTITY, DTO_PARTIAL> getMapper(Class<DTO_PARTIAL> dto_partialClass) {
        return DtoMappers.get(entityClass, dto_partialClass);
    }

    protected boolean isNew(Long id) {
        return id == null || id.equals(0L);
    }

    protected boolean isNew(ENTITY dto) {
        return dto.getId() == null || dto.getId().equals(0L);
    }
}
