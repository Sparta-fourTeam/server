package com.novaserver.clientpolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientPolicySourceTest {
    @TempDir Path directory;

    @Test
    void publicStatusDoesNotSerializeDerivedInternalFlags() throws Exception {
        var reader = new com.fasterxml.jackson.databind.ObjectMapper();
        String legacy = reader.writeValueAsString(ClientPolicy.disabled());
        String mvc =
                tools.jackson.databind.json.JsonMapper.builder()
                        .build()
                        .writeValueAsString(ClientPolicy.disabled());
        assertThat(reader.readTree(legacy).has("maintenance")).isFalse();
        assertThat(reader.readTree(mvc)).isEqualTo(reader.readTree(legacy));
    }

    @Test
    void missingDefaultIsOffButExplicitMissingOrCorruptFileFailsClosed() throws Exception {
        assertThat(new ClientPolicySource("").current().enforcementEnabled()).isFalse();
        Path file = directory.resolve("policy.json");
        ClientPolicySource source = new ClientPolicySource(file.toString());
        assertThat(source.current().isMaintenance()).isTrue();
        Files.writeString(file, "invalid");
        assertThat(source.current().isMaintenance()).isTrue();
    }

    @Test
    void atomicReplacementIsObservedWithoutStalePolicyCache() throws Exception {
        Path file = directory.resolve("policy.json");
        ClientPolicySource source = new ClientPolicySource(file.toString());
        Files.writeString(file, json("NORMAL", false));
        assertThat(source.current().enforcementEnabled()).isFalse();
        Path replacement = directory.resolve("next.json");
        Files.writeString(replacement, json("MAINTENANCE", true));
        Files.move(
                replacement,
                file,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        assertThat(source.current().isMaintenance()).isTrue();
    }

    @Test
    void shippedTemplateIsNormalOffAndLegacyCompatible() {
        ClientPolicy policy = new ClientPolicySource("deploy/client-policy/policy.json").current();
        assertThat(policy.revision()).isEqualTo("initial-off");
        assertThat(policy.enforcementEnabled()).isFalse();
        assertThat(policy.allowLegacy()).isTrue();
    }

    @Test
    void incompleteBooleansAndUnsafeRevisionFailClosed() throws Exception {
        Path file = directory.resolve("invalid-policy.json");
        ClientPolicySource source = new ClientPolicySource(file.toString());
        for (String invalid :
                new String[] {
                    json("NORMAL", false).replace("\"allowLegacy\":true,", ""),
                    json("NORMAL", false).replace("\"enforcementEnabled\":false,", ""),
                    json("NORMAL", false).replace("\"test\"", "\"bad revision\"")
                }) {
            Files.writeString(file, invalid);
            assertThat(source.current().isMaintenance()).isTrue();
        }
    }

    @Test
    void sharedDirectoryReadersObserveAtomicActivationAndRollback() throws Exception {
        Path file = directory.resolve("shared-policy.json");
        final ClientPolicySource blue = new ClientPolicySource(file.toString());
        final ClientPolicySource green = new ClientPolicySource(file.toString());
        String original = json("NORMAL", false);
        Files.writeString(file, original);
        Path candidate = directory.resolve("candidate.json");
        Files.writeString(
                candidate, json("MAINTENANCE", true).replace("\"test\"", "\"maintenance-v2\""));
        Files.move(
                candidate,
                file,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        assertThat(blue.current().revision()).isEqualTo("maintenance-v2");
        assertThat(green.current()).isEqualTo(blue.current());
        assertThat(Files.readString(file)).contains("maintenance-v2");
        Files.writeString(candidate, original);
        Files.move(
                candidate,
                file,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        assertThat(blue.current().enforcementEnabled()).isFalse();
        assertThat(green.current().revision()).isEqualTo("test");
    }

    private String json(String mode, boolean enabled) {
        return "{\"schemaVersion\":1,\"revision\":\"test\",\"enforcementEnabled\":"
                + enabled
                + ",\"mode\":\""
                + mode
                + "\",\"message\":\"\",\"estimatedEnd\":null,"
                + "\"retryAfterSeconds\":30,\"allowLegacy\":true,\"platforms\":{}}";
    }
}
