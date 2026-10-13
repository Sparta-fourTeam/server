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

import com.novaserver.cards.dto.CardVariantResponse;
import com.novaserver.cards.service.CardVariantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardVariantController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardVariantControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CardVariantService cardVariantService;

    private CardVariantResponse response(Long id, String name) {
        return new CardVariantResponse(id, 5, name, null, List.of());
    }

    @Test
    void returnsVariantList() throws Exception {
        when(cardVariantService.getVariants(1L)).thenReturn(List.of(response(1L, "상위형")));

        mockMvc.perform(get("/api/admin/cards/1/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("상위형"));
    }

    @Test
    void returnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/cards/1/variants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"minPermanentLevel\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsVariantSuccessfully() throws Exception {
        when(cardVariantService.createVariant(eq(1L), any())).thenReturn(response(1L, "상위형"));

        mockMvc.perform(
                        post("/api/admin/cards/1/variants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"minPermanentLevel\":5,\"name\":\"상위형\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("상위형"));
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        when(cardVariantService.updateVariant(eq(1L), any())).thenReturn(response(1L, "변경됨"));

        mockMvc.perform(
                        put("/api/admin/card-variants/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"minPermanentLevel\":9,\"name\":\"변경됨\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("변경됨"));
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/admin/card-variants/1")).andExpect(status().isNoContent());

        verify(cardVariantService).deleteVariant(1L);
    }
}
