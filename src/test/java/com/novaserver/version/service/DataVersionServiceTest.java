package com.novaserver.version.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.version.dto.DataVersionResponse;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DataVersionServiceTest {

    @Mock private DataVersionRepository dataVersionRepository;

    @InjectMocks private DataVersionService dataVersionService;

    @Test
    void returnsVersionOneForAllTablesWhenNoneSaved() {
        when(dataVersionRepository.findAll()).thenReturn(List.of());

        DataVersionResponse response = dataVersionService.getVersions();

        assertThat(response.getRevision()).isEqualTo(1);
        assertThat(response.getTables()).hasSize(DataTable.values().length);
        assertThat(response.getTables().values()).allMatch(v -> v == 1);
    }

    @Test
    void returnsSavedVersionsWhenPresent() {
        DataVersion monsters = mock(DataVersion.class);
        when(monsters.getTableName()).thenReturn(DataTable.MONSTERS.getKey());
        when(monsters.getVersion()).thenReturn(3);
        DataVersion revision = mock(DataVersion.class);
        when(revision.getTableName()).thenReturn(DataVersion.REVISION_KEY);
        when(revision.getVersion()).thenReturn(2);
        when(dataVersionRepository.findAll()).thenReturn(List.of(monsters, revision));

        DataVersionResponse response = dataVersionService.getVersions();

        assertThat(response.getRevision()).isEqualTo(2);
        assertThat(response.getTables().get(DataTable.MONSTERS.getKey())).isEqualTo(3);
        assertThat(response.getTables().get(DataTable.STAGES.getKey())).isEqualTo(1);
    }

    @Test
    void increasesVersionForExistingTable() {
        when(dataVersionRepository.increaseVersion(
                        DataTable.MONSTERS.getKey(), DataVersion.REVISION_KEY))
                .thenReturn(2);

        dataVersionService.increase(DataTable.MONSTERS.getKey());

        verify(dataVersionRepository)
                .increaseVersion(DataTable.MONSTERS.getKey(), DataVersion.REVISION_KEY);
    }

    @Test
    void throwsWhenTableMissing() {
        assertThatThrownBy(() -> dataVersionService.increase("없는테이블"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void throwsWhenVersionRowMissing() {
        when(dataVersionRepository.increaseVersion(eq(DataTable.MONSTERS.getKey()), any()))
                .thenReturn(1);

        assertThatThrownBy(() -> dataVersionService.increase(DataTable.MONSTERS.getKey()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
