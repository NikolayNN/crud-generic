package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceExtCRUD;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/**
 * Abstract controller providing extended CRUD operations for entities with relational dependencies.
 * <p>
 * This controller extends {@link AbsFlexControllerRUD} to add support for creating entities that are
 * directly associated with another specified entity, identified by a relationship ID (`Ext_ID`). It's
 * designed for scenarios where entities cannot exist independently but are part of a larger relationship
 * context, such as components within a larger mechanism.
 * </p>
 * <p>
 * The creation operation is augmented to include the relationship ID as part of the request, ensuring
 * that newly created entities are immediately linked to their respective parent or related entity. This
 * class includes pre-save and post-save hooks that allow for additional processing or validation to be
 * performed with the context of the relationship.
 * </p>
 *
 * @param <ID>            The type of the primary entity's identifier.
 * @param <READ_DTO>      The DTO type for read operations, must extend {@link AbstractDto}.
 * @param <READ_DTO_VIEW> The view type to be returned after read operations.
 * @param <UPDATE_DTO>    The DTO type for update operations, must extend {@link AbsUpdateDto}.
 * @param <CREATE_DTO>    The DTO type for create operations, must extend {@link AbsCreateDto}.
 * @param <SETTINGS>      The settings applied during operations, extending a base settings class.
 * @param <SERVICE>       The service providing the extended CRUD functionality.
 * @param <Ext_ID>        The type of the identifier for the related entity.
 */
public abstract class AbsFlexControllerExtCRUD<
        ID,
        READ_DTO extends AbstractDto<ID>,
        READ_DTO_VIEW,
        UPDATE_DTO extends AbsUpdateDto<ID>,
        CREATE_DTO extends AbsCreateDto,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceExtCRUD<ID, ?, READ_DTO, UPDATE_DTO, CREATE_DTO, ?, Ext_ID, ?>,
        Ext_ID>

        extends AbsFlexControllerRUD<ID, READ_DTO, READ_DTO_VIEW, UPDATE_DTO, SETTINGS, SERVICE> {

    public AbsFlexControllerExtCRUD(SERVICE service) {
        super(service);
    }

    /**
     * Creates a new entity with a specified relation to another entity.
     * <p>
     * This method handles the POST request to create a new entity that is associated with
     * another entity identified by the {@code relationId}. It invokes before and after save
     * hooks for custom logic and validation related to the relationship context.
     * </p>
     *
     * @param relationId The ID of the related entity to which the new entity will be associated.
     * @param body       The DTO containing the data for the new entity.
     * @param settings   The operation settings.
     * @param request    The HttpServletRequest providing request context.
     * @return A ResponseEntity containing the created entity's view.
     */
    public ResponseEntity<READ_DTO_VIEW> save(Ext_ID relationId,
                                              CREATE_DTO body,
                                              SETTINGS settings,
                                              HttpServletRequest request) {
        beforeSaveHook(relationId, body, request);
        READ_DTO saved = service.save(relationId, body);
        afterSaveHook(relationId, body, request);
        return okResponse(saved, settings);
    }

    /**
     * Hook method executed before creating a new entity with a specified relation.
     * <p>
     * Can be overridden in subclasses to perform validation or other preprocessing related to the
     * relationship ID and the creation DTO. Useful for access control checks or pre-populating
     * fields based on the related entity.
     * </p>
     *
     * @param relationId The ID of the related entity.
     * @param obj        The creation DTO.
     * @param request    The HttpServletRequest.
     * @throws AuthenticationException If authentication fails.
     */
    protected void beforeSaveHook(Ext_ID relationId, CREATE_DTO obj, HttpServletRequest request) throws AuthenticationException {
    }

    /**
     * Hook method executed after a new entity with a specified relation is created.
     * <p>
     * Can be overridden in subclasses for post-creation processing, such as logging or triggering
     * further actions based on the new entity's relationship.
     * </p>
     *
     * @param relationId The ID of the related entity.
     * @param body       The creation DTO used for the entity creation.
     */
    protected void afterSaveHook(Ext_ID relationId, CREATE_DTO body, HttpServletRequest request) {
    }
}
