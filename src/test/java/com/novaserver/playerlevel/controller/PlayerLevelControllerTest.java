package com.novaserver.playerlevel.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.playerlevel.dto.PlayerLevelConfigResponse;
import com.novaserver.playerlevel.service.PlayerLevelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PlayerLevelController.class, PlayerLevelAdminController.class})
@AutoConfigureMockMvc(addFilters = false)
class PlayerLevelControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PlayerLevelService service;

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"baseExp\":10,\"maxLevel\":4}",
                "{\"stepExp\":0,\"maxLevel\":4}",
                "{\"baseExp\":10,\"stepExp\":0}",
                "{\"baseExp\":10,\"stepExp\":-1,\"maxLevel\":4}",
                "{\"baseExp\":1,\"stepExp\":0,\"maxLevel\":2147483647}"
            })
    void rejectsMissingOrInvalidFieldsWithoutSaving(String body) throws Exception {
        mvc.perform(
                        put("/api/admin/player-levels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void exposesStoredCurveAndItsVersion() throws Exception {
        when(service.getConfig()).thenReturn(new PlayerLevelConfigResponse(10, 5, 4, 2));
        mvc.perform(get("/api/player-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseExp").value(10))
                .andExpect(jsonPath("$.stepExp").value(5))
                .andExpect(jsonPath("$.maxLevel").value(4))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void rejectsUnconfiguredCurveWithoutFallback() throws Exception {
        when(service.getConfig()).thenThrow(ErrorCode.PLAYER_LEVELS_NOT_CONFIGURED.exception());
        mvc.perform(get("/api/player-levels")).andExpect(status().isServiceUnavailable());
    }
}
