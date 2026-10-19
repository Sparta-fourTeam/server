package com.novaserver.monster.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.monster.service.MonsterCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonsterDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class MonsterDataControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private MonsterCatalog catalog;

    @Test
    void returnsClientTableJsonAndSameMonstersVersionAsVersionEndpoint() throws Exception {
        when(catalog.download())
                .thenReturn(
                        new MonsterCatalog.Download(
                                "[{\"Id\":1,\"Hp\":36,\"Grade\":\"Normal\"}]", 3, "a".repeat(64)));

        mockMvc.perform(get("/api/data/monsters"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Data-Version", "3"))
                .andExpect(jsonPath("$[0].Id").value(1))
                .andExpect(jsonPath("$[0].Grade").value("Normal"));
    }
}
