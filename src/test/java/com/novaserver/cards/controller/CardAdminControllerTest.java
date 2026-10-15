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

import com.novaserver.cards.dto.CardDetailResponse;
import com.novaserver.cards.dto.CardSummaryResponse;
import com.novaserver.cards.service.CardService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardAdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CardService cardService;

    private CardDetailResponse detailResponse(Long id, String name) {
        return new CardDetailResponse(
                id,
                new CardDetailResponse.NamedRef(1L, "화염검"),
                name,
                null,
                1,
                true,
                null,
                1,
                0,
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of());
    }

    @Test
    void returnsCardList() throws Exception {
        when(cardService.getCards())
                .thenReturn(List.of(new CardSummaryResponse(1L, "카드", 1L, "화염검", true, 1)));

        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("카드"));
    }

    @Test
    void returnsCardDetail() throws Exception {
        when(cardService.getCard(1L)).thenReturn(detailResponse(1L, "카드"));

        mockMvc.perform(get("/api/admin/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("카드"));
    }

    @Test
    void returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"skillId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenMaxPickCountBelowOne() throws Exception {
        mockMvc.perform(
                        post("/api/admin/cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"skillId\":1,\"name\":\"카드\",\"maxPickCount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenMinPermanentLevelNegative() throws Exception {
        mockMvc.perform(
                        post("/api/admin/cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"skillId\":1,\"name\":\"카드\","
                                                + "\"minPermanentLevel\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsCardSuccessfully() throws Exception {
        when(cardService.createCard(any())).thenReturn(detailResponse(1L, "카드"));

        mockMvc.perform(
                        post("/api/admin/cards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"skillId\":1,\"name\":\"카드\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(cardService.updateCard(eq(1L), any())).thenReturn(detailResponse(1L, "변경된 카드"));

        mockMvc.perform(
                        put("/api/admin/cards/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"skillId\":1,\"name\":\"변경된 카드\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("변경된 카드"));
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/admin/cards/1")).andExpect(status().isNoContent());

        verify(cardService).deleteCard(1L);
    }
}
