package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.util.PageableUtils;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
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
    protected final SPECS filterSpecs;

    public AbsFlexPagingAndSortingService(JpaSpecificationExecutor<ENTITY> repository, SPECS filterSpecs) {
        this.repository = repository;
        this.filterSpecs = filterSpecs;
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

    public Page<DTO> page(PageFilterRequest request) {
        Specification<ENTITY> entitySpecification = null;
        if (!request.getFilterGroup().isEmpty()) {
            entitySpecification = buildSpecs(request);
        }

        Pageable pageable = PageableUtils.buildPageRequest(request.getPage(), request.getPageSize(), filterSpecs.handleSort(request));

        return repository.findAll(entitySpecification, pageable)
                .map(this::toDto);
    }

    protected abstract DTO toDto(ENTITY entity);

    protected abstract Optional<Specification<ENTITY>> buildSpecification(PageFilterRequest.Filter filter);

}

