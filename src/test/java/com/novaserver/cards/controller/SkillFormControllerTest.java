package com.novaserver.cards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.cards.dto.SkillFormResponse;
import com.novaserver.cards.entity.SkillForm;
import com.novaserver.cards.service.SkillFormService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SkillFormController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkillFormControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SkillFormService skillFormService;

    @Test
    void returnsFormList() throws Exception {
        when(skillFormService.getForms())
                .thenReturn(List.of(new SkillFormResponse(new SkillForm("beast", "짐승형"))));

        mockMvc.perform(get("/api/admin/skill-forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("beast"));
    }

    @Test
    void returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/skill-forms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"짐승형\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsFormSuccessfully() throws Exception {
        when(skillFormService.createForm(any()))
                .thenReturn(new SkillFormResponse(new SkillForm("beast", "짐승형")));

        mockMvc.perform(
                        post("/api/admin/skill-forms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"beast\",\"label\":\"짐승형\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("beast"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(skillFormService.updateForm(eq(1L), any()))
                .thenReturn(new SkillFormResponse(new SkillForm("plant", "식물형")));

        mockMvc.perform(
                        put("/api/admin/skill-forms/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"plant\",\"label\":\"식물형\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("plant"));
    }
}
