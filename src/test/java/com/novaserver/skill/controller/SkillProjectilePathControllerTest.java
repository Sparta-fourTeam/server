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

import com.novaserver.skill.dto.SkillProjectilePathResponse;
import com.novaserver.skill.entity.SkillProjectilePath;
import com.novaserver.skill.service.SkillProjectilePathService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SkillProjectilePathController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkillProjectilePathControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SkillProjectilePathService skillProjectilePathService;

    @Test
    void returnsPathList() throws Exception {
        when(skillProjectilePathService.getSkillProjectilePaths())
                .thenReturn(
                        List.of(
                                new SkillProjectilePathResponse(
                                        new SkillProjectilePath("Straight", "직선"))));

        mockMvc.perform(get("/api/skill-projectile-paths"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Straight"));
    }

    @Test
    void returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(
                        post("/api/skill-projectile-paths")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"직선\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsPathSuccessfully() throws Exception {
        when(skillProjectilePathService.createSkillProjectilePath(any()))
                .thenReturn(
                        new SkillProjectilePathResponse(new SkillProjectilePath("Straight", "직선")));

        mockMvc.perform(
                        post("/api/skill-projectile-paths")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Straight\",\"label\":\"직선\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Straight"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(skillProjectilePathService.updateSkillProjectilePath(eq(1L), any()))
                .thenReturn(
                        new SkillProjectilePathResponse(new SkillProjectilePath("Curve", "곡선")));

        mockMvc.perform(
                        put("/api/skill-projectile-paths/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Curve\",\"label\":\"곡선\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Curve"));
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/skill-projectile-paths/1")).andExpect(status().isNoContent());

        verify(skillProjectilePathService).deleteSkillProjectilePath(1L);
    }
}
