package com.novaserver.version.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataTableTest {

    @Test
    void findsTableByKey() {
        Optional<DataTable> found = DataTable.fromKey("Monsters");

        assertThat(found).contains(DataTable.MONSTERS);
    }

    @Test
    void returnsEmptyWhenKeyMissing() {
        Optional<DataTable> found = DataTable.fromKey("없는테이블");

        assertThat(found).isEmpty();
    }
}
