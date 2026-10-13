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

import com.novaserver.cards.dto.RequiredCardCountResponse;
import com.novaserver.cards.service.RequiredCardCountService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RequiredCardCountController.class)
@AutoConfigureMockMvc(addFilters = false)
class RequiredCardCountControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RequiredCardCountService requiredCardCountService;

    private RequiredCardCountResponse response(Long id, int count) {
        return new RequiredCardCountResponse(
                id, new RequiredCardCountResponse.NamedRef(2L, "선행카드"), null, count);
    }

    @Test
    void returnsRequiredCardCountList() throws Exception {
        when(requiredCardCountService.getRequiredCardCounts(1L))
                .thenReturn(List.of(response(1L, 2)));

        mockMvc.perform(get("/api/admin/cards/1/required-card-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(2));
    }

    @Test
    void returnsBadRequestWhenRequiredCardIdMissing() throws Exception {
        mockMvc.perform(
                        post("/api/admin/cards/1/required-card-counts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"count\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsRequiredCardCountSuccessfully() throws Exception {
        when(requiredCardCountService.createRequiredCardCount(eq(1L), any()))
                .thenReturn(response(1L, 2));

        mockMvc.perform(
                        post("/api/admin/cards/1/required-card-counts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"requiredCardId\":2,\"count\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void returnsNoContentOnDelete() throws Exception {
        mockMvc.perform(delete("/api/admin/required-card-counts/1"))
                .andExpect(status().isNoContent());

        verify(requiredCardCountService).deleteRequiredCardCount(1L);
    }
}
