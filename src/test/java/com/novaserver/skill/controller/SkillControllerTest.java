package com.novaserver.skill.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.skill.dto.SkillDataResponse;
import com.novaserver.skill.service.SkillService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SkillController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkillControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SkillService skillService;

    @Test
    void returnsSkillDataForUnity() throws Exception {
        when(skillService.getSkillData())
                .thenReturn(
                        List.of(
                                new SkillDataResponse(
                                        1L,
                                        "Melee",
                                        "Straight",
                                        null,
                                        5,
                                        false,
                                        null,
                                        1,
                                        null,
                                        List.of(),
                                        List.of())));

        mockMvc.perform(get("/api/skill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].castType").value("Melee"));
    }
}
