package com.apimarketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:apimarketplace;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "app.security.jwt.secret=TestSecretKeyForBackendTestSecretKeyForBackendTest",
    "app.security.jwt.access-token-minutes=15",
    "app.security.jwt.refresh-token-days=30",
    "app.kyc.provider.enabled=false"
})
@AutoConfigureMockMvc
class ApiMarketplaceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void refreshTokenRotationAndLogoutRevokeSession() throws Exception {
        AuthTokens initialTokens = registerProvider();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(initialTokens.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshExpiresInSeconds").value(2592000))
            .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedAccessToken = refreshBody.path("accessToken").asText();
        String rotatedRefreshToken = refreshBody.path("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(initialTokens.refreshToken())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + rotatedAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(rotatedRefreshToken)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(rotatedRefreshToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    void providerCallbackUpdatesKycVerificationState() throws Exception {
        AuthTokens tokens = registerProvider();
        MvcResult submitResult = mockMvc.perform(post("/api/compliance/kyc/submit")
                .header("Authorization", "Bearer " + tokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "legalBusinessName": "Callback Provider Private Limited",
                      "contactName": "Ritesh Sharma",
                      "email": "%s",
                      "phoneNumber": "9876543210",
                      "businessType": "Business",
                      "panNumber": "ABCDE1234F",
                      "gstin": "22ABCDE1234F1Z5",
                      "bankAccountNumber": "123456789012",
                      "bankIfsc": "HDFC0ABC123",
                      "verificationMethods": ["BANK_ACCOUNT", "PAN"],
                      "consentGiven": true,
                      "registeredAddress": "Bengaluru"
                    }
                    """.formatted(tokens.email())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn();

        JsonNode submitBody = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String applicationId = submitBody.path("id").asText();

        mockMvc.perform(post("/api/compliance/kyc/provider/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "applicationId": "%s",
                      "referenceId": "AADHAARKYC-REF-%s",
                      "method": "PAN",
                      "status": "FAILED",
                      "verified": false,
                      "message": "PAN name mismatch detected by provider",
                      "score": 0.21
                    }
                    """.formatted(applicationId, UUID.randomUUID().toString().substring(0, 8).toUpperCase())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.panVerified").value(false))
            .andExpect(jsonPath("$.status").value("NEEDS_MORE_INFO"))
            .andExpect(jsonPath("$.verificationProvider").value("AADHAARKYC"))
            .andExpect(jsonPath("$.verificationReference").isNotEmpty())
            .andExpect(jsonPath("$.verificationSummary").value(org.hamcrest.Matchers.containsString("PAN=FAILED")));

        mockMvc.perform(get("/api/compliance/kyc/me")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.panVerified").value(false))
            .andExpect(jsonPath("$.status").value("NEEDS_MORE_INFO"))
            .andExpect(jsonPath("$.verificationReferences[0]").isNotEmpty());
    }

    private AuthTokens registerProvider() throws Exception {
        String email = "provider-" + UUID.randomUUID().toString().substring(0, 8) + "@apimarketplace.local";
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "Provider@1234",
                      "fullName": "Demo Provider",
                      "companyName": "FinEdge Systems",
                      "role": "PROVIDER"
                    }
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        JsonNode registerBody = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return new AuthTokens(
            email,
            registerBody.path("accessToken").asText(),
            registerBody.path("refreshToken").asText()
        );
    }

    private record AuthTokens(String email, String accessToken, String refreshToken) {}
}
