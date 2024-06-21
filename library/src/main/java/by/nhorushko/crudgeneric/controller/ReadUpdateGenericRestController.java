package by.nhorushko.crudgeneric.controller;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.service.RudGenericService;
import by.nhorushko.filterspecification.FilterSpecificationAbstract;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Deprecated
public abstract class ReadUpdateGenericRestController<
        DTO_INTERMEDIATE extends AbstractDto,
        DTO_VIEW extends AbstractDto,
        ENTITY extends AbstractEntity,
        SETTINGS extends SettingsVoid,
        CRUD_SERVICE extends RudGenericService<DTO_INTERMEDIATE, ENTITY, ?, ?>>
        extends PageableGenericRestController<DTO_INTERMEDIATE, DTO_VIEW, ENTITY, SETTINGS, CRUD_SERVICE> {

    public ReadUpdateGenericRestController(CRUD_SERVICE service, FilterSpecificationAbstract<ENTITY> filterSpecs) {
        super(service, filterSpecs);
    }

    @PutMapping("{id}")
    public ResponseEntity<DTO_VIEW> update(@PathVariable("id") Long id,
                                      SETTINGS settings,
                                      @RequestBody DTO_INTERMEDIATE obj,
                                      HttpServletRequest request) {

        if (!id.equals(obj.getId())) {
            throw new IllegalArgumentException("wrong id");
        }
        checkAccessUpdateBefore(obj, request);
        DTO_INTERMEDIATE saved = service.update(obj);
        return okResponse(saved, settings);
    }

    protected void checkAccessUpdateBefore(DTO_INTERMEDIATE obj, HttpServletRequest request) {
        checkAccess(request, obj);
    }
}
