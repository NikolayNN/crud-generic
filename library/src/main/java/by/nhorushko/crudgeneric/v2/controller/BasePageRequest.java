package by.nhorushko.crudgeneric.v2.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * Base class for pagination requests. This class encapsulates common pagination
 * and sorting parameters that can be used across different API endpoints to
 * standardize request formats for fetching paged resources.
 *
 * Fields include the page number, page size, and a sort criterion, each of which
 * influences how the data is retrieved and presented in a paged response. This
 * class is designed to be extended by specific request classes that might require
 * additional filtering or sorting parameters.
 *
 * To customize default values for these parameters in a subclass, override the
 * default constructor and set new default values. Example:
 *
 * <pre>{@code
 * public class CustomPageRequest extends BasePageRequest {
 *     public CustomPageRequest() {
 *         this.page = 1;  // Set default page number to 1
 *         this.size = 25; // Set default page size to 25
 *         this.sort = "-customField"; // Set default sort criterion
 *     }
 * }
 * }</pre>
 *
 * This approach allows subclasses to define their own default values for pagination
 * and sorting while still leveraging the common structure provided by BasePageRequest.
 */
@Data
public class BasePageRequest {

    /**
     * The page number of the requested page in a paged query. Pagination starts at 0.
     * This parameter allows clients to request a specific page of data from a collection.
     */
    @Parameter(description = "request page")
    protected int page = 0;

    /**
     * The size of the page, indicating the number of records to be returned in a single page.
     * This parameter allows clients to specify the volume of data they wish to receive.
     */
    @Parameter(description = "page size")
    protected int size = 20;

    /**
     * The sorting criterion used to order the data returned in the page. The format for this
     * parameter is a sort key prefixed with an optional "-" for descending order. By default,
     * records are sorted by the ID in descending order.
     *
     * Example: "-startTime" sorts by startTime in descending order, while "startTime" sorts
     * in ascending order.
     */
    @Parameter(description = "sort Criteria. Example: sort=-startTime [startTime, status]")
    protected String sort = "-id";
}
