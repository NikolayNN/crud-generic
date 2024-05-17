package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Provides Read and Update functionality for a specific entity type.
 * <p>
 * This controller extends {@link AbsFlexControllerR} by adding the ability to update an existing
 * entity. It handles HTTP PUT requests by validating the provided entity update DTO against the
 * specified ID in the path variable. If validation passes, the entity is updated in the service layer.
 * Pre-update and post-update hook methods are provided to allow for custom logic before and after
 * the update operation.
 * </p>
 *
 * @param <ID>            the type of the entity's identifier
 * @param <READ_DTO>      the type of the read DTO
 * @param <READ_DTO_VIEW> the view type returned after read operations
 * @param <UPDATE_DTO>    the type of the update DTO
 * @param <SETTINGS>      the settings applied during operations
 * @param <SERVICE>       the service providing read and update functionality
 */
public abstract class AbsFlexControllerRU<
        ID,
        READ_DTO extends AbstractDto<ID>,
        READ_DTO_VIEW,
        UPDATE_DTO extends AbsUpdateDto<ID>,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceRUD<ID, ?, READ_DTO, UPDATE_DTO, ?>>
        extends AbsFlexControllerR<ID, READ_DTO, READ_DTO_VIEW, SETTINGS, SERVICE> {

    public AbsFlexControllerRU(SERVICE service) {
        super(service);
    }

    /**
     * Updates an entity identified by the given ID with the provided update DTO.
     * <p>
     * Validates that the ID in the path matches the ID in the DTO. If the IDs match, it processes
     * the update operation, invoking pre-update and post-update hooks for custom logic execution.
     * </p>
     *
     * @param id       the ID of the entity to update, as specified in the path
     * @param settings settings to apply during the update operation
     * @param obj      the update DTO containing the new entity data
     * @param request  the current HttpServletRequest
     * @return a ResponseEntity containing the updated entity view
     * @throws IllegalArgumentException if the provided ID does not match the ID in the DTO
     */
    @PutMapping("{id}")
    public ResponseEntity<READ_DTO_VIEW> update(@PathVariable("id") ID id,
                                                SETTINGS settings,
                                                @Valid @RequestBody UPDATE_DTO obj,
                                                HttpServletRequest request) {
        if (!id.equals(obj.getId())) {
            throw new IllegalArgumentException("wrong id");
        }
        beforeUpdateHook(obj, request);
        READ_DTO saved = service.update(obj);
        afterUpdateHook(obj, request);
        return okResponse(saved, settings);
    }

    /**
     * Hook method executed before an entity is updated.
     * <p>
     * Can be overridden in subclasses to add logic before the update operation, such as validation
     * or logging.
     * </p>
     *
     * @param obj     the DTO containing the update data
     * @param request the current HttpServletRequest
     * @throws AuthenticationException if there are authentication issues during post-processing
     */
    protected void beforeUpdateHook(UPDATE_DTO obj, HttpServletRequest request) throws AuthenticationException {
    }

    /**
     * Hook method executed after an entity is updated.
     * <p>
     * Can be overridden in subclasses to add logic after the update operation, such as clearing
     * caches or additional logging.
     * </p>
     *
     * @param obj     the DTO containing the update data
     * @param request the current HttpServletRequest
     */
    protected void afterUpdateHook(UPDATE_DTO obj, HttpServletRequest request) {
    }
}
