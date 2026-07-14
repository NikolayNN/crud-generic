package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import by.nhorushko.filterspecification.Converters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.mapping.PropertyReferenceException;

import java.util.ArrayList;
import java.util.List;

/**
 * Base service for paged, filtered, sorted reads. Subclasses declare their
 * filterable fields once in {@link #filterFields(FilterFields.Builder)};
 * validation, specification building and sort mapping all derive from that
 * single declaration.
 */
public abstract class AbsFlexPagingAndSortingService<
        ID,
        DTO extends AbstractDto<ID>,
        ENTITY extends AbstractEntity<ID>> {

    protected final JpaSpecificationExecutor<ENTITY> repository;
    protected final AbsModelMapper mapper;
    private final Class<DTO> dtoClass;
    private final Converters converters;
    private volatile FilterFields<ENTITY> filterFields;

    public AbsFlexPagingAndSortingService(JpaSpecificationExecutor<ENTITY> repository,
                                          AbsModelMapper mapper,
                                          Class<DTO> dtoClass) {
        this(repository, mapper, dtoClass, null);
    }

    /**
     * Use this constructor only when {@link #filterFields(FilterFields.Builder)}
     * declares {@code field()} entries, which resolve their converter from the
     * application's {@link Converters} subclass.
     */
    public AbsFlexPagingAndSortingService(JpaSpecificationExecutor<ENTITY> repository,
                                          AbsModelMapper mapper,
                                          Class<DTO> dtoClass,
                                          Converters converters) {
        this.repository = repository;
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.converters = converters;
    }

    public Page<DTO> page(PageFilterRequest request) {
        FilterFields<ENTITY> fields = fields();
        Specification<ENTITY> specification = buildSpecFromFilterGroup(fields, request.getFilterGroup());
        Pageable pageable = PageRequest.of(request.getPage(), request.getPageSize(), fields.sort(request.getSort()));
        try {
            return repository.findAll(specification, pageable).map(this::toDto);
        } catch (PropertyReferenceException e) {
            throw new FilterValidationException("Unknown sort property in request: " + request.getSort(), e);
        }
    }

    /**
     * Declares the filterable fields of this endpoint — the single place a
     * field's name, path, type and allowed operations are defined. Called
     * once; the result is cached.
     */
    protected abstract FilterFields<ENTITY> filterFields(FilterFields.Builder<ENTITY> builder);

    protected DTO toDto(ENTITY entity) {
        return mapper.map(entity, dtoClass);
    }

    private FilterFields<ENTITY> fields() {
        FilterFields<ENTITY> result = filterFields;
        if (result == null) {
            synchronized (this) {
                result = filterFields;
                if (result == null) {
                    FilterFields.Builder<ENTITY> builder = converters == null
                            ? FilterFields.builder()
                            : FilterFields.builder(converters);
                    result = filterFields(builder);
                    filterFields = result;
                }
            }
        }
        return result;
    }

    private Specification<ENTITY> buildSpecFromFilterGroup(FilterFields<ENTITY> fields,
                                                           PageFilterRequest.FilterGroup filterGroup) {
        if (filterGroup == null || filterGroup.isEmpty()) {
            return null;
        }

        List<Specification<ENTITY>> specs = new ArrayList<>();

        for (PageFilterRequest.Filter filter : filterGroup.getFilters()) {
            fields.toSpecification(filter).ifPresent(specs::add);
        }

        for (PageFilterRequest.FilterGroup subgroup : filterGroup.getSubGroups()) {
            Specification<ENTITY> subgroupSpec = buildSpecFromFilterGroup(fields, subgroup);
            if (subgroupSpec != null) {
                specs.add(subgroupSpec);
            }
        }

        if (specs.isEmpty()) {
            return null;
        }

        Specification<ENTITY> resultSpec = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            resultSpec = filterGroup.getCondition() == PageFilterRequest.ConcatCondition.AND
                    ? resultSpec.and(specs.get(i))
                    : resultSpec.or(specs.get(i));
        }
        return resultSpec;
    }
}
