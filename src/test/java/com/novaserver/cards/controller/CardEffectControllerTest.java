package com.novaserver.cards.controller;

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

import com.novaserver.cards.dto.CardEffectResponse;
import com.novaserver.cards.service.CardEffectService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardEffectController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardEffectControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CardEffectService cardEffectService;

    private CardEffectResponse response(Long id, String kind) {
        return new CardEffectResponse(
                id, 0, kind, 10.0, null, null, null, null, 1.0, 1.0, null, null, 1, null, false,
                null, null);
    }

    @Test
    void returnsCardEffectList() throws Exception {
        when(cardEffectService.getCardEffects(1L)).thenReturn(List.of(response(1L, "damage")));

        mockMvc.perform(get("/api/admin/cards/1/effects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("damage"));
    }

    @Test
    void returnsBadRequestWhenKindMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/cards/1/effects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsCardEffectSuccessfully() throws Exception {
        when(cardEffectService.createCardEffect(eq(1L), any())).thenReturn(response(1L, "damage"));

        mockMvc.perform(
                        post("/api/admin/cards/1/effects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sortOrder\":0,\"kind\":\"damage\",\"value\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("damage"));
    }

    @Test
    void returnsVariantEffectList() throws Exception {
        when(cardEffectService.getVariantEffects(1L)).thenReturn(List.of(response(1L, "damage")));

        mockMvc.perform(get("/api/admin/card-variants/1/effects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("damage"));
    }

    @Test
    void createsVariantEffectSuccessfully() throws Exception {
        when(cardEffectService.createVariantEffect(eq(1L), any()))
                .thenReturn(response(1L, "damage"));

        mockMvc.perform(
                        post("/api/admin/card-variants/1/effects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sortOrder\":0,\"kind\":\"damage\",\"value\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("damage"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(cardEffectService.updateEffect(eq(1L), any())).thenReturn(response(1L, "heal"));

        mockMvc.perform(
                        put("/api/admin/card-effects/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sortOrder\":0,\"kind\":\"heal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("heal"));
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/admin/card-effects/1")).andExpect(status().isNoContent());

        verify(cardEffectService).deleteEffect(1L);
    }
}
