package com.novaserver.monster.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.monster.service.MonsterCatalog;
import com.novaserver.version.dto.DataVersionResponse;
import com.novaserver.version.service.DataVersionService;
import java.util.Map;
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

    @MockitoBean private DataVersionService versions;

    @Test
    void returnsClientTableJsonAndSameMonstersVersionAsVersionEndpoint() throws Exception {
        when(catalog.json()).thenReturn("[{\"Id\":1,\"Hp\":36,\"Grade\":\"Normal\"}]");
        when(versions.getVersions()).thenReturn(new DataVersionResponse(4, Map.of("Monsters", 3)));

        mockMvc.perform(get("/api/data/monsters"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Data-Version", "3"))
                .andExpect(jsonPath("$[0].Id").value(1))
                .andExpect(jsonPath("$[0].Grade").value("Normal"));
    }
}
