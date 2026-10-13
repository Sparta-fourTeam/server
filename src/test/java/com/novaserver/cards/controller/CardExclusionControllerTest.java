package com.novaserver.cards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.cards.dto.CardExclusionResponse;
import com.novaserver.cards.service.CardExclusionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardExclusionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardExclusionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CardExclusionService cardExclusionService;

    private CardExclusionResponse response(Long id, int belowPermanentLevel) {
        return new CardExclusionResponse(
                id, new CardExclusionResponse.NamedRef(2L, "배제카드"), null, belowPermanentLevel);
    }

    @Test
    void returnsExclusionList() throws Exception {
        when(cardExclusionService.getExclusions(1L)).thenReturn(List.of(response(1L, 25)));

        mockMvc.perform(get("/api/admin/cards/1/exclusions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].belowPermanentLevel").value(25));
    }

    @Test
    void returnsBadRequestWhenExcludingCardIdMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/cards/1/exclusions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"belowPermanentLevel\":25}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsExclusionSuccessfully() throws Exception {
        when(cardExclusionService.createExclusion(eq(1L), any())).thenReturn(response(1L, 25));

        mockMvc.perform(
                        post("/api/admin/cards/1/exclusions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"excludingCardId\":2,\"belowPermanentLevel\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.belowPermanentLevel").value(25));
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/admin/card-exclusions/1")).andExpect(status().isNoContent());

        verify(cardExclusionService).deleteExclusion(1L);
    }
}
