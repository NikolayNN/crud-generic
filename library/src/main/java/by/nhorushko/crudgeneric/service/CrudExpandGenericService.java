package by.nhorushko.crudgeneric.service;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

@Deprecated
public abstract class CrudExpandGenericService<
        DTO extends AbstractDto,
        ENTITY extends AbstractEntity,
        REPOSITORY extends JpaRepository<ENTITY, Long> & JpaSpecificationExecutor<ENTITY>,
        MAPPER extends AbstractMapper<ENTITY, DTO>> extends CrudAdditionalGenericService<DTO, ENTITY, REPOSITORY, MAPPER> {

    public CrudExpandGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(repository, mapper, dtoClass, entityClass);
    }

    @Override
    public DTO getById(Long id) {
        ENTITY e = findEntityById(id);
        if (e != null) {
            return mapper.toDto(e);
        } else {
            return createEmptyDto(id);
        }
    }

    @Override
    public @Nullable
    ENTITY findEntityById(Long id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * в общем случае сделать setId();
     */
    protected abstract DTO createEmptyDto(Long id);

    @Override
    /**
     * use update instead
     */
    public DTO save(Long rootId, DTO dto) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void checkIdForSave(ENTITY e) {
        //do nothing
    }

    @Override
    protected void setupEntityBeforeSave(Long rootId, ENTITY entity) {
        //do nothing
    }
}
