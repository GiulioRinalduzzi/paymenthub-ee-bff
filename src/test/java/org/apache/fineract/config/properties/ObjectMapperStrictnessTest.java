package org.apache.fineract.config.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Date;
import org.apache.fineract.operations.Transfer;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * The ObjectMapper bean used to be a plain new ObjectMapper(). These tests fix the two things about
 * it that are visible on the wire, so the move to a configured builder can be checked without
 * deploying anything: the JSON written for a Transfer is unchanged, and a body with an unknown field
 * is still refused.
 */
class ObjectMapperStrictnessTest {

    private static final ObjectMapper PREVIOUS_BEHAVIOUR = new ObjectMapper();

    private static ObjectMapper currentBean() {
        return Jackson2ObjectMapperBuilder.json().build().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Test
    void writesATransferExactlyAsBefore() throws Exception {
        Transfer transfer = new Transfer();
        transfer.setTransactionId("abc-123");
        transfer.setStartedAt(new Date(1758000000000L));
        transfer.setCompletedAt(new Date(1758000240000L));

        assertEquals(PREVIOUS_BEHAVIOUR.writeValueAsString(transfer), currentBean().writeValueAsString(transfer));
    }

    @Test
    void stillRefusesABodyWithAnUnknownField() {
        String body = "{\"transactionId\":\"abc-123\",\"somethingNew\":1}";

        assertThrows(UnrecognizedPropertyException.class, () -> PREVIOUS_BEHAVIOUR.readValue(body, Transfer.class));
        assertThrows(UnrecognizedPropertyException.class, () -> currentBean().readValue(body, Transfer.class));
    }
}
