package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Abstract service class providing read-only operations for entities.
 * <p>
 * This abstract class is designed to encapsulate common read-only operations for entities, including fetching
 * by ID and checking for existence. It utilizes Spring Data JPA's {@link JpaRepository} for repository operations
 * and leverages a custom {@link AbsDtoModelMapper} for DTO to entity mapping. It is generic and can be used with
 * any entity that extends {@link AbstractEntity} and any DTO that extends {@link AbstractDto}.
 * </p>
 * <p>
 * Implementations of this class need to specify the entity type {@code ENTITY}, the DTO type {@code READ_DTO} for
 * read operations, the ID type {@code ID}, and the repository type {@code REPOSITORY} that extends
 * {@link JpaRepository}. This setup allows for flexible and type-safe querying and data transfer operations.
 * </p>
 *
 * @param <ID>        the type of the entity's identifier
 * @param <ENTITY>    the entity type that extends {@link AbstractEntity}
 * @param <READ_DTO>  the DTO type used for read operations, extending {@link AbstractDto}
 * @param <REPOSITORY> the repository type for the entity, extending {@link JpaRepository}
 *
 * @see AbstractEntity
 * @see AbstractDto
 * @see JpaRepository
 */
@Transactional
public abstract class AbsFlexServiceR<
        ID,
        ENTITY extends AbstractEntity<ID>,
        READ_DTO extends AbstractDto<ID>,
        REPOSITORY extends JpaRepository<ENTITY, ID>> {

    protected final AbsDtoModelMapper mapper;

    protected final REPOSITORY repository;

    @Getter
    protected final Class<ENTITY> entityClass;

    @Getter
    protected final Class<READ_DTO> readDtoClass;

    public AbsFlexServiceR(AbsDtoModelMapper mapper,
                           REPOSITORY repository,
                           Class<ENTITY> entityClass,
                           Class<READ_DTO> readDtoClass) {
        this.mapper = mapper;
        this.repository = repository;
        this.entityClass = entityClass;
        this.readDtoClass = readDtoClass;
    }

    /**
     * Retrieves an entity by its ID and returns an optional DTO representation.
     * <p>
     * This method fetches an entity based on the provided ID. If the entity is found, it is mapped to its
     * corresponding DTO type and returned inside an {@link Optional}. If not found, an empty {@link Optional}
     * is returned. This approach allows for graceful handling of cases where the entity might not exist.
     * </p>
     *
     * @param id the ID of the entity to retrieve
     * @return an {@link Optional} containing the mapped DTO if the entity is found, or an empty {@link Optional} if not found
     */
    public Optional<READ_DTO> getByIdOptional(ID id) {
        return repository.findById(id)
                .map(this::mapReadDto);
    }

    /**
     * Retrieves multiple entities by their IDs and returns a list of DTO representations.
     * <p>
     * This method fetches a list of entities based on the provided collection of IDs. Each retrieved entity is
     * then mapped to its corresponding DTO type. This is useful for bulk retrieval operations.
     * </p>
     *
     * @param ids the collection of IDs of the entities to retrieve
     * @return a list of DTOs representing the retrieved entities
     */
    public List<READ_DTO> getById(Collection<ID> ids) {
        List<ENTITY> entities = repository.findAllById(ids);
        return mapAllReadDto(entities);
    }

    /**
     * Retrieves an entity by its ID and returns its DTO representation.
     * <p>
     * This method fetches an entity based on the provided ID and maps it to its corresponding DTO type. If the entity
     * is not found, it throws an {@link AppNotFoundException}. This is suitable for cases where the existence of the
     * entity is assumed and non-existence is treated as an exceptional condition.
     * </p>
     *
     * @param id the ID of the entity to retrieve
     * @return the DTO representation of the entity
     * @throws AppNotFoundException if the entity with the specified ID is not found
     */
    public READ_DTO getById(ID id) {
        return getByIdOptional(id)
                .orElseThrow(() -> new AppNotFoundException(format("Entity id: %s was not found", id)));
    }

    /**
     * Checks if an entity with the specified ID exists.
     * <p>
     * This method verifies the existence of an entity based on the provided ID. It is useful for existence checks
     * before proceeding with operations that require the entity to be present.
     * </p>
     *
     * @param id the ID of the entity to check
     * @return {@code true} if an entity with the specified ID exists, {@code false} otherwise
     */
    public boolean isExist(ID id) {
        return repository.existsById(id);
    }

    /**
     * Maps an entity to its corresponding READ_DTO representation.
     * <p>
     * This protected method is used internally to convert an entity to its corresponding DTO representation.
     * It leverages the configured {@link AbsDtoModelMapper} for the conversion.
     * </p>
     *
     * @param entity the entity to map
     * @return the DTO representation of the entity
     */
    protected READ_DTO mapReadDto(ENTITY entity) {
        return this.mapper.map(entity, readDtoClass);
    }

    /**
     * Maps a collection of entities to their corresponding READ_DTO representations.
     * <p>
     * This protected method is used internally to convert a collection of entities to a list of their corresponding
     * DTO representations. It is useful for bulk mapping operations.
     * </p>
     *
     * @param entities the collection of entities to map
     * @return a list of DTOs representing the entities
     */
    protected List<READ_DTO> mapAllReadDto(Collection<ENTITY> entities) {
        return this.mapper.mapAll(entities, readDtoClass);
    }
}
