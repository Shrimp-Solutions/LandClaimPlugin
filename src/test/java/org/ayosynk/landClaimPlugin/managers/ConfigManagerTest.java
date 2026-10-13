package org.ayosynk.landClaimPlugin.managers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigManagerTest {

    @Test
    void configuredProfileLimitAppliesWithoutPermissionOverride() {
        assertEquals(2, ConfigManager.resolveMaxProfiles(2, List.of()));
    }

    @Test
    void highestNumericPermissionRaisesConfiguredProfileLimit() {
        assertEquals(5, ConfigManager.resolveMaxProfiles(
                2,
                List.of("landclaim.profiles.limit.5", "landclaim.profiles.limit.invalid")));
    }

    @Test
    void lowerOrUnrelatedPermissionDoesNotReduceConfiguredLimit() {
        assertEquals(5, ConfigManager.resolveMaxProfiles(
                5,
                List.of("landclaim.profiles.limit.2", "landclaim.limit.100")));
    }
}
