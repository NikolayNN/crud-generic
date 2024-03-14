package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Abstract controller providing standardized read operations for a specified entity type.
 * <p>
 * This controller abstracts common read functionalities such as fetching by ID and converting the fetched
 * entity's data transfer object (DTO) to a view-specific DTO. It leverages an associated read service
 * defined by the generic parameter {@code SERVICE} for data retrieval and encapsulates the conversion
 * logic necessary to prepare the response in the desired format.
 * </p>
 *
 * @param <ID>       the type of the entity's identifier
 * @param <DTO>      the type of the Data Transfer Object used by the service, extending {@link AbstractDto}
 * @param <DTO_VIEW> the type of the view-specific DTO to be returned by the controller methods
 * @param <SETTINGS> the settings type that might influence the handling of the request or response
 * @param <SERVICE>  the service type used for read operations, extending {@link AbsFlexServiceR}
 */
public abstract class AbsFlexControllerR<ID, DTO extends AbstractDto<ID>, DTO_VIEW, SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceR<ID, ?, DTO, ?>> {

    protected final SERVICE service;

    public AbsFlexControllerR(SERVICE service) {
        this.service = service;
    }

    /**
     * Handles the GET request to retrieve an entity by its ID and convert it to a view-specific DTO.
     * <p>
     * This method fetches an entity by its ID, applies pre- and post-retrieval hooks, and then converts
     * the service layer DTO to the view-specific DTO format before sending it as the response.
     * </p>
     *
     * @param id       the ID of the entity to retrieve
     * @param settings the settings to possibly influence the handling
     * @param request  the current HTTP request
     * @return a {@link ResponseEntity} wrapping the view-specific DTO
     */
    @GetMapping("{id}")
    @Operation(summary = "Get by id")
    public ResponseEntity<DTO_VIEW> getById(@PathVariable("id") ID id,
                                            SETTINGS settings,
                                            HttpServletRequest request) {
        beforeGetByIdHook(id, request);
        DTO dto = service.getById(id);
        afterGetByIdHook(dto, request);
        return okResponse(dto, settings);
    }

    /**
     * Pre-processes the request before retrieving an entity by ID.
     * <p>
     * This hook method is called prior to retrieving an entity from the service layer.
     * It provides an opportunity to implement custom pre-retrieval logic, such as validation,
     * logging, authentication checks, or access control verifications, specific to the getById operation.
     * This method can be particularly useful for ensuring that the current user has the necessary permissions
     * to access the requested entity, thereby serving as a critical checkpoint for enforcing security and
     * access control policies within the application.
     * </p>
     *
     * @param id      the ID of the entity to be retrieved
     * @param request the current HttpServletRequest, which can be analyzed to make security or access decisions
     * @throws AuthenticationException if there are authentication issues or access violations during pre-processing
     */
    protected void beforeGetByIdHook(ID id, HttpServletRequest request) throws AuthenticationException {
    }

    /**
     * Post-processes the data after retrieving an entity by ID.
     * <p>
     * This hook method is invoked after an entity is retrieved from the service layer but before
     * the response is sent. It allows for custom logic to be executed, such as modifying the DTO
     * or performing additional checks or logging.
     * </p>
     *
     * @param dto     the retrieved DTO
     * @param request the current HttpServletRequest
     * @throws AuthenticationException if there are authentication issues during post-processing
     */
    protected void afterGetByIdHook(DTO dto, HttpServletRequest request) throws AuthenticationException {
    }

    /**
     * Wraps the DTO in a ResponseEntity with HTTP status OK.
     * <p>
     * This method converts a single DTO to its corresponding DTO_VIEW using the {@code postHandle} method
     * and wraps the result in a ResponseEntity. It is used to generate standard HTTP OK responses for single
     * resource retrievals.
     * </p>
     *
     * @param dtoIntermediate the DTO to be converted and wrapped
     * @param settings        the settings to be applied during conversion
     * @return a ResponseEntity containing the DTO_VIEW and HTTP status OK
     */
    protected ResponseEntity<DTO_VIEW> okResponse(DTO dtoIntermediate, SETTINGS settings) {
        DTO_VIEW dtoView = postHandle(dtoIntermediate, settings);
        return ResponseEntity.ok(dtoView);
    }

    /**
     * Wraps a collection of DTOs in a ResponseEntity with HTTP status OK.
     * <p>
     * Converts a collection of DTOs to a list of DTO_VIEWs using the {@code postHandle} method
     * and wraps the result in a ResponseEntity. It is used for generating standard HTTP OK responses
     * for retrievals returning multiple resources.
     * </p>
     *
     * @param list     the collection of DTOs to be converted
     * @param settings the settings to be applied during conversion
     * @return a ResponseEntity containing a list of DTO_VIEWs and HTTP status OK
     */
    protected ResponseEntity<List<DTO_VIEW>> okResponse(Collection<DTO> list, SETTINGS settings) {
        List<DTO_VIEW> result = list.stream().map(d -> postHandle(d, settings)).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Converts a DTO to its corresponding view representation (DTO_VIEW).
     * <p>
     * Subclasses must implement this method to define the conversion logic from DTO to DTO_VIEW,
     * possibly applying settings as part of the conversion process.
     * </p>
     *
     * @param dto      the DTO to convert
     * @param settings the settings influencing the conversion
     * @return the converted DTO_VIEW
     */
    protected abstract DTO_VIEW postHandle(DTO dto, SETTINGS settings);

    /**
     * Converts a collection of DTOs to their corresponding view representations (DTO_VIEW).
     * <p>
     * This method applies the {@code postHandle} conversion to each DTO in the collection,
     * facilitating bulk conversions of DTOs to DTO_VIEWs.
     * </p>
     *
     * @param dtos     the collection of DTOs to convert
     * @param settings the settings influencing the conversion
     * @return a list of converted DTO_VIEWs
     */
    protected List<DTO_VIEW> postHandle(Collection<DTO> dtos, SETTINGS settings) {
        return dtos.stream().map(d -> postHandle(d, settings)).collect(Collectors.toList());
    }
}
