package com.loadup.assessment.order.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TenantFilterTest {
    @Test
    void missingTenantHeaderReturnsBadRequestWithoutCallingFilterChain() throws Exception {
        TenantFilter tenantFilter = new TenantFilter((request, response, handler, exception) -> {
            response.setStatus(400);
            return new ModelAndView();
        });
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean filterChainCalled = new AtomicBoolean();

        tenantFilter.doFilter(request, response, (servletRequest, servletResponse) -> filterChainCalled.set(true));

        assertEquals(400, response.getStatus());
        assertFalse(filterChainCalled.get());
    }
}
