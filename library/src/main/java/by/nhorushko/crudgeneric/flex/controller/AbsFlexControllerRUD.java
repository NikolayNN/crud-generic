package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controller providing Read, Update, and Delete (RUD) operations for a specific entity type.
 * <p>
 * This class extends {@link AbsFlexControllerRU} by incorporating the delete operation, allowing clients to
 * remove entities from the system. It handles HTTP DELETE requests by invoking the corresponding service
 * layer method to delete the entity identified by the given ID. Customizable pre-delete and post-delete
 * hook methods are provided to facilitate additional processing or validation before and after the delete
 * operation.
 * </p>
 *
 * @param <ID>          the type of the entity's identifier
 * @param <READ_DTO>    the DTO type for read operations, extending {@link AbstractDto}
 * @param <DTO_VIEW>    the view type returned after read and update operations
 * @param <UPDATE_DTO>  the DTO type for update operations, extending {@link AbsUpdateDto}
 * @param <SETTINGS>    the settings type for operation customization
 * @param <SERVICE>     the service type providing RUD functionality
 */
public abstract class AbsFlexControllerRUD<
        ID,
        READ_DTO extends AbstractDto<ID>,
        DTO_VIEW,
        UPDATE_DTO extends AbsUpdateDto<ID>,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceRUD<ID, ?, READ_DTO, UPDATE_DTO, ?>>
        extends AbsFlexControllerRU<ID, READ_DTO, DTO_VIEW, UPDATE_DTO, SETTINGS, SERVICE> {

    public AbsFlexControllerRUD(SERVICE service) {
        super(service);
    }

    /**
     * Handles the HTTP DELETE request to delete an entity by its ID.
     * <p>
     * This method invokes pre-delete and post-delete hooks for additional processing around the
     * deletion operation. It then calls the service layer to delete the entity identified by the
     * provided ID. A response with HTTP status 204 No Content is returned upon successful deletion.
     * </p>
     *
     * @param id      The ID of the entity to be deleted.
     * @param request The HttpServletRequest, allowing access to the request details.
     * @return A ResponseEntity representing an empty body with HTTP status 204 No Content.
     */
    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") ID id, HttpServletRequest request) {
        beforeDeleteHook(id, request);
        service.delete(id);
        afterDeleteHook(id, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Hook method executed before an entity is deleted.
     * <p>
     * Can be overridden in subclasses to implement logic prior to the delete operation, such as
     * authorization checks, validation, or logging.
     * </p>
     *
     * @param id      The ID of the entity to be deleted.
     * @param request The HttpServletRequest, providing context for the deletion.
     * @throws AuthenticationException if there are authentication issues during post-processing
     */
    protected void beforeDeleteHook(ID id, HttpServletRequest request) throws AuthenticationException {
    }

    /**
     * Hook method executed after an entity is deleted.
     * <p>
     * Can be overridden in subclasses to perform actions following the delete operation, such as
     * clearing related caches, logging, or triggering other system events.
     * </p>
     *
     * @param id      The ID of the entity that was deleted.
     * @param request The HttpServletRequest, providing context for the deletion.
     */
    protected void afterDeleteHook(ID id, HttpServletRequest request) {
    }
}
