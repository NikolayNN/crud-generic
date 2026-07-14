package by.nhorushko.crudgeneric.flex.exception;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * AppNotFoundException is thrown from service update/delete/getById paths;
 * without @ResponseStatus a Spring MVC app turns it into HTTP 500 instead of 404.
 */
public class AppNotFoundExceptionTest {

    @Test
    public void mapsToHttpNotFound() {
        ResponseStatus responseStatus = AppNotFoundException.class.getAnnotation(ResponseStatus.class);

        assertNotNull("AppNotFoundException must be annotated with @ResponseStatus", responseStatus);
        assertEquals(HttpStatus.NOT_FOUND, responseStatus.value());
    }
}
