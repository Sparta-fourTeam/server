package com.novaserver.cards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.cards.dto.EffectTriggerResponse;
import com.novaserver.cards.entity.EffectTrigger;
import com.novaserver.cards.service.EffectTriggerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EffectTriggerController.class)
@AutoConfigureMockMvc(addFilters = false)
class EffectTriggerControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EffectTriggerService effectTriggerService;

    @Test
    void returnsTriggerList() throws Exception {
        when(effectTriggerService.getTriggers())
                .thenReturn(List.of(new EffectTriggerResponse(new EffectTrigger("onHit", "피격 시"))));

        mockMvc.perform(get("/api/admin/effect-triggers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("onHit"));
    }

    @Test
    void returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/effect-triggers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"피격 시\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsTriggerSuccessfully() throws Exception {
        when(effectTriggerService.createTrigger(any()))
                .thenReturn(new EffectTriggerResponse(new EffectTrigger("onHit", "피격 시")));

        mockMvc.perform(
                        post("/api/admin/effect-triggers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"onHit\",\"label\":\"피격 시\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("onHit"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(effectTriggerService.updateTrigger(eq(1L), any()))
                .thenReturn(new EffectTriggerResponse(new EffectTrigger("onDeath", "사망 시")));

        mockMvc.perform(
                        put("/api/admin/effect-triggers/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"onDeath\",\"label\":\"사망 시\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("onDeath"));
    }
}
