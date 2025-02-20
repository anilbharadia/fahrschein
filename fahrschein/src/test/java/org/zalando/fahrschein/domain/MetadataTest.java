package org.zalando.fahrschein.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataTest {

    @Test
    public void shouldHaveCleanStringRepresentation() {
        final String eventType = "TEST-EVENT-TYPE";
        final String eid = "a3e25946-5ae9-3964-91fa-26ecb7588d67";
        final String partition = "partition1";
        final String version = "v1";
        final String publishedBy = "unauthenticated";
        final OffsetDateTime occurredAt = OffsetDateTime.now();
        final OffsetDateTime receivedAt = OffsetDateTime.now();
        final String flowId = UUID.randomUUID().toString();

        final Metadata metadata = new Metadata(eventType, eid, occurredAt, partition, version, publishedBy, receivedAt, flowId, Collections.emptyMap());

        assertTrue(metadata.toString().contains(metadata.getEventType()));
        assertTrue(metadata.toString().contains(metadata.getEid()));
        assertTrue(metadata.toString().contains(metadata.getOccurredAt().toString()));
        assertTrue(metadata.toString().contains(metadata.getPartition()));
        assertTrue(metadata.toString().contains(metadata.getVersion()));
        assertTrue(metadata.toString().contains(metadata.getPublishedBy()));
        assertTrue(metadata.toString().contains(metadata.getReceivedAt().toString()));
        assertTrue(metadata.toString().contains(metadata.getFlowId()));
    }
}
