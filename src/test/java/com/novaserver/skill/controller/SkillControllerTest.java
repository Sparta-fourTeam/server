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

import com.novaserver.skill.dto.SkillDataResponse;
import com.novaserver.skill.dto.SkillDetailResponse;
import com.novaserver.skill.dto.SkillSummaryResponse;
import com.novaserver.skill.service.SkillService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SkillController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkillControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SkillService skillService;

    private SkillDetailResponse detailResponse() {
        return new SkillDetailResponse(
                1L,
                "파이어볼",
                "설명",
                new SkillDetailResponse.NamedRef(10L, "Melee"),
                new SkillDetailResponse.NamedRef(20L, "Straight"),
                5,
                false,
                null,
                1);
    }

    @Test
    void returnsSkillList() throws Exception {
        when(skillService.getSkillList())
                .thenReturn(
                        List.of(
                                new SkillSummaryResponse(
                                        1L, "파이어볼", "Melee", "Straight", List.of("cast"))));

        mockMvc.perform(get("/api/skill/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("파이어볼"))
                .andExpect(jsonPath("$[0].statGroups[0]").value("cast"));
    }

    @Test
    void returnsSkillDetail() throws Exception {
        when(skillService.getSkillDetail(1L)).thenReturn(detailResponse());

        mockMvc.perform(get("/api/skill/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("파이어볼"))
                .andExpect(jsonPath("$.castType.name").value("Melee"));
    }

    @Test
    void returnsSkillDataForUnity() throws Exception {
        when(skillService.getSkillData())
                .thenReturn(
                        List.of(
                                new SkillDataResponse(
                                        1L, "파이어볼", "설명", "Melee", "Straight", 5, false, null, 1)));

        mockMvc.perform(get("/api/skill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].castType").value("Melee"));
    }

    @Test
    void returnsBadRequestWhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/api/skill").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsSkillSuccessfully() throws Exception {
        when(skillService.createSkill(any())).thenReturn(detailResponse());

        mockMvc.perform(
                        post("/api/skill")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"파이어볼\",\"castTypeId\":10,"
                                                + "\"projectilePathId\":20,\"maxLevel\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("파이어볼"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(skillService.updateSkill(eq(1L), any())).thenReturn(detailResponse());

        mockMvc.perform(
                        put("/api/skill/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"파이어볼\",\"castTypeId\":10,"
                                                + "\"projectilePathId\":20,\"maxLevel\":5}"))
                .andExpect(status().isOk());

        verify(skillService).updateSkill(eq(1L), any());
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/skill/1")).andExpect(status().isNoContent());

        verify(skillService).deleteSkill(1L);
    }
}
