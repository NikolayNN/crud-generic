package by.nhorushko.crudgeneric.controller;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.service.CrudExpandGenericService;
import by.nhorushko.filterspecification.FilterSpecificationAbstract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;


@Deprecated
public abstract class CrudExpandRestController<
        DTO_INTERMEDIATE extends AbstractDto,
        DTO_VIEW extends AbstractDto,
        ENTITY extends AbstractEntity,
        SETTINGS extends SettingsVoid,
        CRUD_SERVICE extends CrudExpandGenericService<DTO_INTERMEDIATE, ENTITY, ?, ?>>
        extends CrudAdditionalRestController<DTO_INTERMEDIATE, DTO_VIEW, ENTITY, SETTINGS, CRUD_SERVICE> {

    public CrudExpandRestController(CRUD_SERVICE service, FilterSpecificationAbstract<ENTITY> filterSpecs) {
        super(service, filterSpecs);
    }

    @Override
    /**
     * use update instead
     */
    public ResponseEntity<DTO_VIEW> save(Long rootId, DTO_INTERMEDIATE body, SETTINGS settings, HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void checkAccessSaveBefore(Long rootId, DTO_INTERMEDIATE obj, HttpServletRequest request) throws AuthenticationException {
        // do nothing
    }
}
