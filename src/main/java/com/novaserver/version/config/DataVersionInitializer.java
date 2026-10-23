package com.novaserver.version.config;

import com.novaserver.clientpolicy.ClientPolicySource;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 서버 시작 시 DB에 없는 버전 행을 버전 1로 만든다. */
@Component
public class DataVersionInitializer implements ApplicationRunner {
    @Autowired(required = false)
    private ClientPolicySource clientPolicy;

    private final DataVersionRepository dataVersionRepository;
    private final TransactionTemplate transactions;
    private volatile boolean initializationPending;

    public DataVersionInitializer(
            DataVersionRepository dataVersionRepository,
            PlatformTransactionManager transactionManager) {
        this.dataVersionRepository = dataVersionRepository;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        initializationPending = true;
        initializeWhenAllowed();
    }

    /** 시작 때 점검으로 미룬 초기화만 재시도한다. commit 후에는 DB를 다시 건드리지 않는다. */
    @Scheduled(fixedDelay = 1000)
    public void retryDeferredInitialization() {
        if (initializationPending) {
            initializeWhenAllowed();
        }
    }

    private synchronized void initializeWhenAllowed() {
        if (!initializationPending) {
            return;
        }
        if (clientPolicy != null && clientPolicy.current().isMaintenance()) {
            return;
        }
        transactions.executeWithoutResult(
                status -> {
                    createIfAbsent(DataVersion.REVISION_KEY);
                    for (DataTable table : DataTable.values()) {
                        createIfAbsent(table.getKey());
                    }
                });
        initializationPending = false;
    }

    private void createIfAbsent(String key) {
        if (!dataVersionRepository.existsById(key)) {
            dataVersionRepository.save(new DataVersion(key));
        }
    }
}
