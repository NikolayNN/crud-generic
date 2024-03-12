package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

public abstract class AbsFlexControllerRU<
        ID,
        READ_DTO extends AbstractDto<ID>,
        READ_DTO_VIEW extends READ_DTO,
        UPDATE_DTO extends AbstractUpdateDto<ID>,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceRUD<ID, ?, READ_DTO, UPDATE_DTO, ?>>
        extends AbsFlexControllerR<ID, READ_DTO, READ_DTO_VIEW, SETTINGS, SERVICE> {

    public AbsFlexControllerRU(SERVICE service) {
        super(service);
    }

    @PutMapping("{id}")
    public ResponseEntity<READ_DTO_VIEW> update(@PathVariable("id") ID id,
                                                SETTINGS settings,
                                                @Valid @RequestBody UPDATE_DTO obj,
                                                HttpServletRequest request) {
        if (!id.equals(obj.getId())) {
            throw new IllegalArgumentException("wrong id");
        }
        checkAccessUpdateBefore(obj, request);
        READ_DTO saved = service.update(obj);
        return okResponse(saved, settings);
    }

    protected void checkAccessUpdateBefore(UPDATE_DTO obj, HttpServletRequest request) {
    }
}
