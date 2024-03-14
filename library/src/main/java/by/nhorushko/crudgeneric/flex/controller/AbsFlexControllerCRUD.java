package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * Controller providing Create, Read, Update, and Delete (CRUD) operations for a specific entity type.
 * <p>
 * This class extends {@link AbsFlexControllerRUD} by adding the ability to create new entities via HTTP POST requests.
 * It accepts a DTO for the entity to be created, utilizes services to perform the creation, and returns the created
 * entity as a DTO view. Customizable pre-save and post-save hook methods are provided to enable additional processing
 * or validation before and after the entity is created.
 * </p>
 *
 * @param <ID>          the type of the entity's identifier
 * @param <READ_DTO>    the DTO type for read operations, extending {@link AbstractDto}
 * @param <DTO_VIEW>    the view type returned after read, update, and create operations
 * @param <UPDATE_DTO>  the DTO type for update operations, extending {@link AbsUpdateDto}
 * @param <CREATE_DTO>  the DTO type for create operations, extending {@link AbsCreateDto}
 * @param <SETTINGS>    the settings type for operation customization
 * @param <SERVICE>     the service type providing CRUD functionality
 */
public abstract class AbsFlexControllerCRUD<
        ID,
        READ_DTO extends AbstractDto<ID>,
        DTO_VIEW,
        UPDATE_DTO extends AbsUpdateDto<ID>,
        CREATE_DTO extends AbsCreateDto,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceCRUD<ID, ?, READ_DTO, UPDATE_DTO, CREATE_DTO, ?>>
        extends AbsFlexControllerRUD<ID, READ_DTO, DTO_VIEW, UPDATE_DTO, SETTINGS, SERVICE> {

    public AbsFlexControllerCRUD(SERVICE service) {
        super(service);
    }

    /**
     * Handles the HTTP POST request to create a new entity.
     * <p>
     * This method invokes pre-save and post-save hooks for additional processing around the
     * creation operation. It then calls the service layer to create the entity based on the
     * provided DTO. A response with the created entity view and HTTP status 201 Created is
     * returned upon successful creation.
     * </p>
     *
     * @param obj      The create DTO containing the data for the new entity.
     * @param settings The settings to apply during the creation operation.
     * @param request  The current HttpServletRequest.
     * @return A ResponseEntity containing the created entity view.
     */
    @PostMapping
    public ResponseEntity<DTO_VIEW> save(@Valid @RequestBody CREATE_DTO obj,
                                         SETTINGS settings,
                                         HttpServletRequest request) {
        beforeSaveHook(obj, request);
        READ_DTO saved = service.save(obj);
        afterSaveHook(obj, request);
        return okResponse(saved, settings);
    }

    /**
     * Hook method executed before an entity is created.
     * <p>
     * Can be overridden in subclasses to implement logic prior to the save operation, such as
     * validation checks, logging, or authentication. This method offers a way to intercept
     * the creation process, allowing for custom behavior or validation to be applied.
     * </p>
     *
     * @param obj     The DTO containing the data for the new entity.
     * @param request The HttpServletRequest, providing context for the creation.
     * @throws AuthenticationException If there are authentication issues during pre-processing.
     */
    protected void beforeSaveHook(CREATE_DTO obj, HttpServletRequest request) throws AuthenticationException {
    }

    /**
     * Hook method executed after an entity is created.
     * <p>
     * Can be overridden in subclasses to perform actions following the save operation, such as
     * clearing caches, logging, or triggering other system events. This method provides a
     * post-creation interception point for additional processing or follow-up actions.
     * </p>
     *
     * @param obj     The DTO containing the data for the new entity.
     * @param request The HttpServletRequest, providing context for the creation.
     */
    protected void afterSaveHook(CREATE_DTO obj, HttpServletRequest request) {
    }
}
