package com.trackingworld.tw.ingest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestHumaniseTest {

    @Test
    void humanisesCodes() {
        assertThat(IngestService.humanise("OVER_SPEED")).isEqualTo("Over speed");
        assertThat(IngestService.humanise("sos")).isEqualTo("Sos");
        assertThat(IngestService.humanise("GEOFENCE-OUT")).isEqualTo("Geofence out");
    }
}
