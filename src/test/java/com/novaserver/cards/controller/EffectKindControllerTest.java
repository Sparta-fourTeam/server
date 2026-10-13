package com.novaserver.cards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.cards.dto.EffectKindResponse;
import com.novaserver.cards.entity.EffectKind;
import com.novaserver.cards.service.EffectKindService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EffectKindController.class)
@AutoConfigureMockMvc(addFilters = false)
class EffectKindControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EffectKindService effectKindService;

    @Test
    void returnsKindList() throws Exception {
        when(effectKindService.getKinds())
                .thenReturn(List.of(new EffectKindResponse(new EffectKind("damage", "피해"))));

        mockMvc.perform(get("/api/admin/effect-kinds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("damage"));
    }

    @Test
    void returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/effect-kinds")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"피해\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsKindSuccessfully() throws Exception {
        when(effectKindService.createKind(any()))
                .thenReturn(new EffectKindResponse(new EffectKind("damage", "피해")));

        mockMvc.perform(
                        post("/api/admin/effect-kinds")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"damage\",\"label\":\"피해\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("damage"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(effectKindService.updateKind(eq(1L), any()))
                .thenReturn(new EffectKindResponse(new EffectKind("heal", "회복")));

        mockMvc.perform(
                        put("/api/admin/effect-kinds/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"heal\",\"label\":\"회복\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("heal"));
    }
}
