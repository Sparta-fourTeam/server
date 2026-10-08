package com.novaserver.version.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.version.dto.DataVersionResponse;
import com.novaserver.version.service.DataVersionService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DataVersionController.class)
@AutoConfigureMockMvc(addFilters = false)
class DataVersionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DataVersionService dataVersionService;

    @Test
    void returnsVersionSuccessfully() throws Exception {
        when(dataVersionService.getVersions())
                .thenReturn(new DataVersionResponse(3, Map.of("Monsters", 2)));

        mockMvc.perform(get("/api/data/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(3))
                .andExpect(jsonPath("$.tables.Monsters").value(2));
    }

    @Test
    void returnsBadRequestWhenTableBlank() throws Exception {
        mockMvc.perform(
                        post("/api/data/version")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"table\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delegatesUpdateRequestToService() throws Exception {
        mockMvc.perform(
                        post("/api/data/version")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"table\":\"Monsters\"}"))
                .andExpect(status().isOk());

        verify(dataVersionService).increase("Monsters");
    }
}
