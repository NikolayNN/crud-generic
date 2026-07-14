package by.nhorushko.crudgenerictest.pageable;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import by.nhorushko.crudgenerictest.domain.entity.MeetingStatus;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import by.nhorushko.crudgenerictest.repository.MeetingRepository;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import by.nhorushko.crudgenerictest.service.MeetingPageableService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingPageIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private MeetingPageableService service;

    private RegionEntity minsk;
    private RegionEntity vitebsk;

    @BeforeEach
    void seed() {
        minsk = regionRepository.save(new RegionEntity(101L, "minsk"));
        vitebsk = regionRepository.save(new RegionEntity(102L, "vitebsk"));
        meetingRepository.save(MeetingEntity.builder()
                .title("sprint planning").status(MeetingStatus.PLANNED)
                .startTime(Instant.parse("2026-07-01T10:00:00Z"))
                .day(LocalDate.parse("2026-07-01")).region(minsk).build());
        meetingRepository.save(MeetingEntity.builder()
                .title("retro").status(MeetingStatus.DONE)
                .startTime(Instant.parse("2026-07-05T10:00:00Z"))
                .day(LocalDate.parse("2026-07-05")).region(minsk).build());
        meetingRepository.save(MeetingEntity.builder()
                .title("planning poker").status(MeetingStatus.CANCELED)
                .startTime(Instant.parse("2026-07-10T10:00:00Z"))
                .day(LocalDate.parse("2026-07-10")).region(vitebsk).build());
    }

    @AfterEach
    void cleanUp() {
        meetingRepository.deleteAll();
        regionRepository.deleteAll();
    }

    @Test
    void filtersByStringContains() throws Exception {
        mockMvc.perform(get("/meeting/page").param("titleFilter", "like#planning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void filtersByEnumEqualAndIn() throws Exception {
        mockMvc.perform(get("/meeting/page").param("statusFilter", "eq#DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("retro"));

        mockMvc.perform(get("/meeting/page").param("statusFilter", "in#PLANNED,DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void filtersByInstantBetween() throws Exception {
        mockMvc.perform(get("/meeting/page")
                        .param("startTimeFilter", "btn#2026-07-01T00:00:00Z,2026-07-06T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void filtersByLocalDateEqual() throws Exception {
        mockMvc.perform(get("/meeting/page").param("dayFilter", "eq#2026-07-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void filtersByNestedRegionId() throws Exception {
        mockMvc.perform(get("/meeting/page").param("regionIdFilter", "eq#101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void combinesFiltersWithAnd() throws Exception {
        mockMvc.perform(get("/meeting/page")
                        .param("titleFilter", "like#planning")
                        .param("regionIdFilter", "eq#101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("sprint planning"));
    }

    @Test
    void sortsByMappedAndPlainProperties() throws Exception {
        mockMvc.perform(get("/meeting/page").param("sort", "asc#startTime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("sprint planning"));

        mockMvc.perform(get("/meeting/page").param("sort", "desc#startTime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("planning poker"));
    }

    @Test
    void sortsByMappedRegionIdProperty() throws Exception {
        // regionId's registry path ("region.id") differs from its name, so this exercises the
        // name->path substitution end-to-end. vitebsk (id 102) holds only "planning poker", so it
        // is unambiguously first under descending regionId. A regression dropping the path
        // substitution would make Sort.by("regionId") fail with PropertyReferenceException (500).
        mockMvc.perform(get("/meeting/page").param("sort", "desc#regionId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].title").value("planning poker"));
    }

    @Test
    void disallowedOperationIsBadRequest() throws Exception {
        mockMvc.perform(get("/meeting/page").param("titleFilter", "eq#retro"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unconvertibleValueIsBadRequest() throws Exception {
        mockMvc.perform(get("/meeting/page").param("regionIdFilter", "eq#abc"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/meeting/page").param("statusFilter", "eq#NO_SUCH"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void legacySortSyntaxIsBadRequest() throws Exception {
        mockMvc.perform(get("/meeting/page").param("sort", "-startTime"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSortPropertyIsBadRequest() throws Exception {
        mockMvc.perform(get("/meeting/page").param("sort", "asc#doesNotExist"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownFilterFieldIsRejectedAtServiceLevel() {
        assertThatThrownBy(() -> service.page(PageFilterRequest.pageRequestAnd(0, 20, "desc#id",
                new PageFilterRequest.Filter("nope", "eq#1"))))
                .isInstanceOf(FilterValidationException.class);
    }
}
