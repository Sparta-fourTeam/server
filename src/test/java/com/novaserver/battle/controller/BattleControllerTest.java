package com.novaserver.battle.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.service.BattleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BattleController.class)
@AutoConfigureMockMvc(addFilters = false)
class BattleControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BattleService battleService;

    @Test
    void startsBattleSuccessfully() throws Exception {
        when(battleService.createBattle(1L)).thenReturn(new BattleResponse("battle-1"));

        mockMvc.perform(
                        post("/api/battle/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.battleId").value("battle-1"));
    }

    @Test
    void returnsBadRequestWhenUserIdMissing() throws Exception {
        mockMvc.perform(
                        post("/api/battle/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delegatesEndRequestToService() throws Exception {
        mockMvc.perform(
                        post("/api/battle/end")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":1,\"battleId\":\"battle-1\"}"))
                .andExpect(status().isOk());

        verify(battleService).endBattle(1L, "battle-1");
    }

    @Test
    void returnsBadRequestWhenBattleIdMissingOnEnd() throws Exception {
        mockMvc.perform(
                        post("/api/battle/end")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":1}"))
                .andExpect(status().isBadRequest());
    }
}
