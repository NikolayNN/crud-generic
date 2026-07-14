package by.nhorushko.crudgeneric.flex.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;

import java.util.Collections;
import java.util.List;

import static by.nhorushko.filterspecification.FilterOperation.CONTAINS;
import static by.nhorushko.filterspecification.FilterOperation.EQUAL;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsFlexPagingAndSortingServiceTest {

    static class PersonEntity implements AbstractEntity<Long> {
        private Long id;

        public PersonEntity(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    static class PersonDto implements AbstractDto<Long> {
        private final Long id;

        PersonDto(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class PersonPageableService extends AbsFlexPagingAndSortingService<Long, PersonDto, PersonEntity> {

        PersonPageableService(JpaSpecificationExecutor<PersonEntity> repository) {
            super(repository, null, PersonDto.class);
        }

        @Override
        protected FilterFields<PersonEntity> filterFields(FilterFields.Builder<PersonEntity> builder) {
            return builder
                    .string("name", CONTAINS)
                    .ofLong("userId", "user.id", EQUAL)
                    .build();
        }

        @Override
        protected PersonDto toDto(PersonEntity entity) {
            return new PersonDto(entity.getId());
        }
    }

    @SuppressWarnings("unchecked")
    private final JpaSpecificationExecutor<PersonEntity> repository = mock(JpaSpecificationExecutor.class);
    private PersonPageableService service;

    @Before
    public void setUp() {
        service = new PersonPageableService(repository);
        // ArgumentMatchers.<T>any() (unlike any(Class)) also matches the null
        // Specification passed when no filters are present.
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<PersonEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new PersonEntity(1L))));
    }

    @Test
    public void pageBuildsPageRequestWithMappedSort() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        service.page(PageFilterRequest.pageRequestAnd(2, 15, "desc#userId",
                new PageFilterRequest.Filter("name", "like#jo")));

        org.mockito.Mockito.verify(repository)
                .findAll(org.mockito.ArgumentMatchers.<Specification<PersonEntity>>any(), pageable.capture());
        assertEquals(PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "user.id")), pageable.getValue());
    }

    @Test
    public void pageWithoutFiltersPassesNullSpecification() {
        ArgumentCaptor<Specification<PersonEntity>> spec = ArgumentCaptor.forClass(Specification.class);

        service.page(PageFilterRequest.pageRequestAnd(0, 20, "asc#name"));

        org.mockito.Mockito.verify(repository).findAll(spec.capture(), any(Pageable.class));
        assertNull(spec.getValue());
    }

    @Test
    public void pageMapsEntitiesWithToDto() {
        assertEquals(Long.valueOf(1L),
                service.page(PageFilterRequest.pageRequestAnd(0, 20, "asc#name"))
                        .getContent().get(0).getId());
    }

    @Test
    public void invalidFilterPropagatesValidationError() {
        assertThrows(FilterValidationException.class,
                () -> service.page(PageFilterRequest.pageRequestAnd(0, 20, "asc#name",
                        new PageFilterRequest.Filter("name", "eq#jo"))));
    }

    @Test
    public void legacySortPropagatesValidationError() {
        assertThrows(FilterValidationException.class,
                () -> service.page(PageFilterRequest.pageRequestAnd(0, 20, "-name")));
    }

    @Test
    public void unknownSortPropertyFromRepositoryPropagatesValidationError() {
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<PersonEntity>>any(), any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("doesNotExist",
                        TypeInformation.of(PersonEntity.class), Collections.emptyList()));

        assertThrows(FilterValidationException.class,
                () -> service.page(PageFilterRequest.pageRequestAnd(0, 20, "asc#doesNotExist")));
    }
}
