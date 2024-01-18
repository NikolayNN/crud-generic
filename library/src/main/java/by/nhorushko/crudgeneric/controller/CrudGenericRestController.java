package by.nhorushko.crudgeneric.controller;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.service.CrudGenericService;
import by.nhorushko.filterspecification.FilterSpecificationAbstract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Deprecated
public abstract class CrudGenericRestController<
        DTO_INTERMEDIATE extends AbstractDto,
        DTO_VIEW extends AbstractDto,
        ENTITY extends AbstractEntity,
        SETTINGS extends SettingsVoid,
        CRUD_SERVICE extends CrudGenericService<DTO_INTERMEDIATE, ENTITY, ?, ?>>
        extends ReadUpdateDeleteGenericRestController<DTO_INTERMEDIATE, DTO_VIEW, ENTITY, SETTINGS, CRUD_SERVICE> {

    public CrudGenericRestController(CRUD_SERVICE service, FilterSpecificationAbstract<ENTITY> filterSpecs) {
        super(service, filterSpecs);
    }

    @PostMapping
    public ResponseEntity<DTO_VIEW> save(@RequestBody DTO_INTERMEDIATE obj,
                                         SETTINGS settings,
                                         HttpServletRequest request) {
        checkAccessSaveBefore(obj, request);
        obj = handleBeforeSave(obj, request, settings);
        DTO_INTERMEDIATE saved = service.save(obj);
        return okResponse(saved, settings);
    }

    protected DTO_INTERMEDIATE handleBeforeSave(DTO_INTERMEDIATE obj, HttpServletRequest request, SETTINGS settings) {
        return obj;
    }

    protected void checkAccessSaveBefore(DTO_INTERMEDIATE obj, HttpServletRequest request) throws AuthenticationException {
        checkAccess(request, obj);
    }
}
