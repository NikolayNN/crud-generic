package by.nhorushko.crudgeneric.controller;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.service.RudGenericService;
import by.nhorushko.filterspecification.FilterSpecificationAbstract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Deprecated
public abstract class ReadUpdateDeleteGenericRestController<
        DTO_INTERMEDIATE extends AbstractDto,
        DTO_VIEW extends AbstractDto,
        ENTITY extends AbstractEntity,
        SETTINGS extends SettingsVoid,
        CRUD_SERVICE extends RudGenericService<DTO_INTERMEDIATE, ENTITY, ?, ?>>
        extends ReadUpdateGenericRestController<DTO_INTERMEDIATE, DTO_VIEW, ENTITY, SETTINGS, CRUD_SERVICE> {

    public ReadUpdateDeleteGenericRestController(CRUD_SERVICE service, FilterSpecificationAbstract<ENTITY> filterSpecs) {
        super(service, filterSpecs);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        checkAccessDeleteBefore(id, request);
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    protected void checkAccessDeleteBefore(Long id, HttpServletRequest request) {
        DTO_INTERMEDIATE dto = service.getById(id);
        checkAccess(request, dto);
    }
}
