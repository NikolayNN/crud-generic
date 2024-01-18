package by.nhorushko.crudgeneric.v2.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceRUD;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class AbsControllerRU<
        ID,
        DTO extends AbstractDto<ID>,
        DTO_VIEW extends DTO,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsServiceRUD<ID, ?, DTO, ?, ?>>
        extends AbsControllerR<ID, DTO, DTO_VIEW, SETTINGS, SERVICE> {

    public AbsControllerRU(SERVICE service) {
        super(service);
    }

    @PutMapping("{id}")
    public ResponseEntity<DTO_VIEW> update(@PathVariable("id") ID id,
                                           SETTINGS settings,
                                           @RequestBody DTO obj,
                                           HttpServletRequest request) {
        if (!id.equals(obj.getId())) {
            throw new IllegalArgumentException("wrong id");
        }
        checkAccessUpdateBefore(obj, request);
        DTO saved = service.update(obj);
        return okResponse(saved, settings);
    }

    protected void checkAccessUpdateBefore(DTO obj, HttpServletRequest request) {
    }
}
