package by.nhorushko.crudgeneric.service;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
public abstract class CrudGenericService<
        DTO extends AbstractDto,
        ENTITY extends AbstractEntity,
        REPOSITORY extends JpaRepository<ENTITY, Long> & JpaSpecificationExecutor<ENTITY>,
        MAPPER extends AbstractMapper<ENTITY, DTO>> extends RudGenericService<DTO, ENTITY, REPOSITORY, MAPPER> {

    protected Set<String> ignorePartialUpdateProperties = Set.of("id");

    public CrudGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(repository, mapper, dtoClass, entityClass);
    }

    public CrudGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass,
                              Set<String> ignorePartialUpdateProperties) {
        super(repository, mapper, dtoClass, entityClass);
        this.ignorePartialUpdateProperties = ignorePartialUpdateProperties;
    }

    public DTO save(DTO dto) {
        check(dto);
        ENTITY entity = mapper.toEntity(dto);
        setEntityIdForSave(entity);
        setupEntityBeforeSave(entity);
        return mapper.toDto(repository.save(entity));
    }

    public List<DTO> saveAll(List<DTO> list) {
        List<ENTITY> entities = list.stream()
                .peek(this::check)
                .map(dto -> {
                    ENTITY e = mapper.toEntity(dto);
                    setEntityIdForSave(e);
                    setupEntityBeforeSave(e);
                    return e;
                }).collect(Collectors.toList());
        return repository.saveAll(entities).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    protected void setupEntityBeforeSave(ENTITY entity) {
    }
}
