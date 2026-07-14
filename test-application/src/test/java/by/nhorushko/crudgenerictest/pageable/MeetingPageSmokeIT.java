package by.nhorushko.crudgenerictest.pageable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingPageSmokeIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void emptyPageIsReturnedWithoutFilters() throws Exception {
        mockMvc.perform(get("/meeting/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
