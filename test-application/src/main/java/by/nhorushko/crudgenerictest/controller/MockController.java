package by.nhorushko.crudgenerictest.controller;

import by.nhorushko.crudgeneric.controller.CrudGenericRestController;
import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgenerictest.MockService;
import by.nhorushko.crudgenerictest.domain.dto.MockADto;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/mock")
@RestController
public class MockController extends CrudGenericRestController<MockADto, MockADto, MockAEntity, SettingsVoid, MockService> {

    public MockController(MockService service) {
        super(service, null);
    }

    @Override
    protected MockADto postHandle(MockADto dto, SettingsVoid settingsVoid) {
        return dto;
    }

    @Override
    protected void checkAccess(HttpServletRequest request, MockADto dto) throws AuthenticationException {
        super.checkAccess(request, dto);
    }
}
