package by.nhorushko.crudgenerictest.controller;

import by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.service.MeetingPageableService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static by.nhorushko.crudgeneric.flex.pageable.PageFilterRequest.pageRequestAnd;

@RestController
@RequestMapping("/meeting")
public class MeetingController {

    private final MeetingPageableService service;

    public MeetingController(MeetingPageableService service) {
        this.service = service;
    }

    @GetMapping("/page")
    public Page<MeetingDto> page(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "desc#id") String sort,
            @RequestParam(value = "titleFilter", required = false) String titleFilter,
            @RequestParam(value = "statusFilter", required = false) String statusFilter,
            @RequestParam(value = "startTimeFilter", required = false) String startTimeFilter,
            @RequestParam(value = "regionIdFilter", required = false) String regionIdFilter,
            @RequestParam(value = "dayFilter", required = false) String dayFilter) {

        PageFilterRequest request = pageRequestAnd(page, size, sort,
                new PageFilterRequest.Filter("title", titleFilter),
                new PageFilterRequest.Filter("status", statusFilter),
                new PageFilterRequest.Filter("startTime", startTimeFilter),
                new PageFilterRequest.Filter("regionId", regionIdFilter),
                new PageFilterRequest.Filter("day", dayFilter));
        return service.page(request);
    }
}
