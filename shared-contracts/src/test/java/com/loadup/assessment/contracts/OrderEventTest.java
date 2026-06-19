package com.loadup.assessment.contracts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderEventTest {
    @Test
    void enumNamesAreStable() {
        assertEquals("ORDER_CREATED", OrderEventType.ORDER_CREATED.name());
    }
}
