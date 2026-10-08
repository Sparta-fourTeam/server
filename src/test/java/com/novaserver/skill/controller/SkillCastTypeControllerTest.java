package com.novaserver.skill.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.skill.dto.SkillCastTypeResponse;
import com.novaserver.skill.entity.SkillCastType;
import com.novaserver.skill.service.SkillCastTypeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SkillCastTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkillCastTypeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SkillCastTypeService skillCastTypeService;

    @Test
    void returnsCastTypeList() throws Exception {
        when(skillCastTypeService.getCastTypes())
                .thenReturn(List.of(new SkillCastTypeResponse(new SkillCastType("Melee", "근접"))));

        mockMvc.perform(get("/api/skill-cast-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Melee"));
    }

    @Test
    void returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(
                        post("/api/skill-cast-types")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"근접\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsCastTypeSuccessfully() throws Exception {
        when(skillCastTypeService.createCastType(any()))
                .thenReturn(new SkillCastTypeResponse(new SkillCastType("Melee", "근접")));

        mockMvc.perform(
                        post("/api/skill-cast-types")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Melee\",\"label\":\"근접\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Melee"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(skillCastTypeService.updateCastType(eq(1L), any()))
                .thenReturn(new SkillCastTypeResponse(new SkillCastType("Ranged", "원거리")));

        mockMvc.perform(
                        put("/api/skill-cast-types/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Ranged\",\"label\":\"원거리\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ranged"));
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/skill-cast-types/1")).andExpect(status().isNoContent());

        verify(skillCastTypeService).deleteCastType(1L);
    }
}
