package com.niniyumi.personalagent.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpaForwardController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpaForwardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void forwardsKnownVueRoutesToTheApplicationEntryPoint() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/weekly-reports/42"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/courses/42"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
