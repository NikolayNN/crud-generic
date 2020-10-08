package by.nhorushko.crudgeneric.controller;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.service.CrudGenericService;
import by.nhorushko.filterspecification.FilterSpecificationAbstract;
import by.nhorushko.filterspecification.FilterSpecificationConstants;
import by.nhorushko.filterspecification.PageRequestBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public class PageableGenericRestController
        <DTO extends AbstractDto, ENTITY extends AbstractEntity,
                CRUD_SERVICE extends CrudGenericService<DTO, ENTITY, ?, ?>> extends ImmutableGenericRestController<DTO, CRUD_SERVICE> {

    protected final static String PARAM_NAME_PAGE = "page";
    protected final static String PARAM_NAME_SIZE = "size";
    protected final static String PARAM_NAME_SORT = "sort";
    protected final static String FILTER_SUFFIX = FilterSpecificationConstants.FILTER_SUFFIX;

    protected FilterSpecificationAbstract<ENTITY> filterSpecs;

    public PageableGenericRestController(CRUD_SERVICE service, FilterSpecificationAbstract<ENTITY> filterSpecs) {
        super(service);
        this.filterSpecs = filterSpecs;
    }

    protected Page<DTO> getPage(int page, int size, String sort, Specification<ENTITY>... specs) {
        if (filterSpecs == null) {
            throw new IllegalArgumentException("Not specify filter specification");
        }
        Specification<ENTITY> rSpecs = buildAndSpecs(specs);
        Pageable pageable = buildPageRequest(page, size, sort, filterSpecs.getEntityFieldPaths());
        return service.list(pageable, rSpecs);
    }

    protected Specification<ENTITY> buildAndSpecs(Specification<ENTITY>... specs) {
        if (specs.length == 0) {
            throw new IllegalArgumentException("expect at least one Specification");
        }
        Specification<ENTITY> result = specs[0];
        for (int i = 1; i < specs.length; i++) {
            result = result.and(specs[i]);
        }
        return Specification.where(result);
    }

    protected Specification<ENTITY> buildOrSpecs(Specification<ENTITY>... specs) {
        if (specs.length == 0) {
            throw new IllegalArgumentException("expect at least one Specification");
        }
        Specification<ENTITY> result = specs[0];
        for (int i = 1; i < specs.length; i++) {
            result = result.or(specs[i]);
        }
        return Specification.where(result);
    }

    protected Pageable buildPageRequest(int page, int size, String sort, Map<String, String> entityFieldPaths) {
        for (String k : entityFieldPaths.keySet()) {
            sort.replaceAll(k, entityFieldPaths.get(k));
        }
        return PageRequestBuilder.getPageRequest(size, page, sort);
    }
}