package by.nhorushko.crudgeneric.v2.pageable;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.util.PageableUtils;
import by.nhorushko.crudgeneric.util.SpecificationUtils;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbsFlexPagingAndSortingService<
        ID,
        DTO extends AbstractDto<ID>,
        ENTITY extends AbstractEntity<ID>,
        SPECS extends AbsFilterSpecification<ENTITY>> {

    protected final JpaSpecificationExecutor<ENTITY> repository;
    protected final AbsModelMapper mapper;
    protected final Class<DTO> dtoClass;
    protected final SPECS filterSpecs;

    public AbsFlexPagingAndSortingService(JpaSpecificationExecutor<ENTITY> repository,
                                          AbsModelMapper mapper,
                                          Class<DTO> dtoClass,
                                          SPECS filterSpecs) {
        this.repository = repository;
        this.mapper = mapper;
        this.filterSpecs = filterSpecs;
        this.dtoClass = dtoClass;
    }

    protected Specification<ENTITY> buildSpecs(PageFilterRequest pageFilterRequest) {
        return buildSpecFromFilterGroup(pageFilterRequest.getFilterGroup());
    }

    protected Specification<ENTITY> buildSpecFromFilterGroup(PageFilterRequest.FilterGroup filterGroup) {
        if (filterGroup == null || filterGroup.isEmpty()) {
            return null;
        }

        List<Specification<ENTITY>> specs = new ArrayList<>();

        // Add specifications for individual filters
        for (PageFilterRequest.Filter filter : filterGroup.getFilters()) {
            buildSpecification(filter).ifPresent(specs::add);
        }

        // Recursively process nested filter groups
        for (PageFilterRequest.FilterGroup subgroup : filterGroup.getSubGroups()) {
            Specification<ENTITY> subgroupSpec = buildSpecFromFilterGroup(subgroup);
            if (subgroupSpec != null) {
                specs.add(subgroupSpec);
            }
        }

        // Combine specifications based on the concat condition
        Specification<ENTITY> resultSpec = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            resultSpec = filterGroup.getCondition() == PageFilterRequest.ConcatCondition.AND
                    ? resultSpec.and(specs.get(i))
                    : resultSpec.or(specs.get(i));
        }

        return resultSpec;
    }

    protected abstract Optional<Specification<ENTITY>> buildSpecification(PageFilterRequest.Filter filter);

    private Specification<ENTITY> concat(Specification<ENTITY>[] specifications, PageFilterRequest.ConcatCondition condition) {
        if (condition == PageFilterRequest.ConcatCondition.AND) {
            return SpecificationUtils.buildAndSpecs(specifications);
        } else {
            return SpecificationUtils.buildOrSpecs(specifications);
        }
    }

    public Page<DTO> page(PageFilterRequest request) {
        Pageable pageable = PageableUtils.buildPageRequest(request.getPage(), request.getPageSize(), filterSpecs.handleSort(request));
        if (request.getFilterGroup().isEmpty()) {
            return repository.findAll(null, pageable)
                    .map(entity -> mapper.map(entity, dtoClass));
        } else {
            return repository.findAll(buildSpecs(request), pageable)
                    .map(entity -> mapper.map(entity, dtoClass));
        }
    }
}

