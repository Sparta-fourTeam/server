package com.novaserver.version.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.clientpolicy.ClientPolicy;
import com.novaserver.clientpolicy.ClientPolicySource;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;

@ExtendWith(MockitoExtension.class)
class DataVersionInitializerTest {

    @Mock private DataVersionRepository dataVersionRepository;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private DataVersionInitializer dataVersionInitializer;

    @Test
    void deferredInitializationRetriesAfterMaintenanceAndStopsAfterCommit() {
        ClientPolicySource policy = org.mockito.Mockito.mock(ClientPolicySource.class);
        ReflectionTestUtils.setField(dataVersionInitializer, "clientPolicy", policy);
        when(policy.current()).thenReturn(ClientPolicy.unavailable());
        dataVersionInitializer
                .retryDeferredInitialization(); // No initialization before ApplicationRunner.
        dataVersionInitializer.run(null);
        dataVersionInitializer.retryDeferredInitialization();
        verifyNoInteractions(dataVersionRepository, transactionManager);
        when(policy.current()).thenReturn(ClientPolicy.disabled());
        dataVersionInitializer.retryDeferredInitialization();
        verify(dataVersionRepository)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                v -> v.getTableName().equals(DataVersion.REVISION_KEY)));
        for (DataTable table : DataTable.values()) {
            verify(dataVersionRepository)
                    .save(
                            org.mockito.ArgumentMatchers.argThat(
                                    v -> v.getTableName().equals(table.getKey())));
        }
        clearInvocations(dataVersionRepository, transactionManager);
        dataVersionInitializer.retryDeferredInitialization();
        verifyNoInteractions(dataVersionRepository, transactionManager);
    }

    @Test
    void failedDeferredCommitRemainsPendingForTheNextRetry() {
        ClientPolicySource policy = org.mockito.Mockito.mock(ClientPolicySource.class);
        ReflectionTestUtils.setField(dataVersionInitializer, "clientPolicy", policy);
        when(policy.current()).thenReturn(ClientPolicy.unavailable());
        dataVersionInitializer.run(null);
        when(policy.current()).thenReturn(ClientPolicy.disabled());
        doThrow(new TransactionSystemException("synthetic commit failure"))
                .doNothing()
                .when(transactionManager)
                .commit(any());
        assertThatThrownBy(dataVersionInitializer::retryDeferredInitialization)
                .isInstanceOf(TransactionSystemException.class);
        clearInvocations(dataVersionRepository);
        dataVersionInitializer.retryDeferredInitialization();
        verify(dataVersionRepository)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                v -> v.getTableName().equals(DataVersion.REVISION_KEY)));
    }

    @Test
    void createsOnlyMissingVersionRows() {
        when(dataVersionRepository.existsById(DataVersion.REVISION_KEY)).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.MONSTERS.getKey())).thenReturn(false);
        when(dataVersionRepository.existsById(DataTable.STAGES.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.COMBAT.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.UPGRADES.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.ENERGY.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.CARDS.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.PLAYER_LEVELS.getKey())).thenReturn(true);
        when(dataVersionRepository.existsById(DataTable.REWARD_RULES.getKey())).thenReturn(true);

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
