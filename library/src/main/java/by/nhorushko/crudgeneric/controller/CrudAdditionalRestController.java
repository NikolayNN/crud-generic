package by.nhorushko.crudgeneric.controller;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.service.CrudAdditionalGenericService;
import by.nhorushko.filterspecification.FilterSpecificationAbstract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;



/**
 *
 */
@Deprecated
public abstract class CrudAdditionalRestController<
        DTO_INTERMEDIATE extends AbstractDto,
        DTO_VIEW extends AbstractDto,
        ENTITY extends AbstractEntity,
        SETTINGS extends SettingsVoid,
        CRUD_SERVICE extends CrudAdditionalGenericService<DTO_INTERMEDIATE, ENTITY, ?, ?>>
        extends ReadUpdateDeleteGenericRestController<DTO_INTERMEDIATE, DTO_VIEW, ENTITY, SETTINGS, CRUD_SERVICE> {

    public CrudAdditionalRestController(CRUD_SERVICE service, FilterSpecificationAbstract<ENTITY> filterSpecs) {
        super(service, filterSpecs);
    }

    /** Example mapping
     * /mechanism/unit/{id}
     */
    public ResponseEntity<DTO_VIEW> save(Long rootId,
                                    DTO_INTERMEDIATE body,
                                    SETTINGS settings,
                                    HttpServletRequest request) {
        checkAccessSaveBefore(rootId, body, request);
        DTO_INTERMEDIATE saved = service.save(rootId, body);
        return okResponse(saved, settings);
    }

    protected abstract void checkAccessSaveBefore(Long rootId, DTO_INTERMEDIATE obj, HttpServletRequest request)
            throws AuthenticationException;
}
