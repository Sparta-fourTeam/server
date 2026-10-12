package com.novaserver.version.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataVersionInitializerTest {

    @Mock private DataVersionRepository dataVersionRepository;

    @InjectMocks private DataVersionInitializer dataVersionInitializer;

    @Test
    void createsOnlyMissingVersionRows() {
        when(dataVersionRepository.existsById(DataVersion.REVISION_KEY)).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.MONSTERS.getKey())).thenReturn(false);
        when(dataVersionRepository.existsById(DataTable.STAGES.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.UPGRADES.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.ENERGY.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.CARDS.getKey())).thenReturn(true);

        dataVersionInitializer.run(null);

        ArgumentCaptor<DataVersion> captor = ArgumentCaptor.forClass(DataVersion.class);
        verify(dataVersionRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getTableName())
                .isEqualTo(DataTable.MONSTERS.getKey());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getVersion()).isEqualTo(1);
    }

    @Test
    void skipsCreationWhenAllRowsExist() {
        when(dataVersionRepository.existsById(any())).thenReturn(true);

        dataVersionInitializer.run(null);

        verify(dataVersionRepository, never()).save(any());
    }
}
