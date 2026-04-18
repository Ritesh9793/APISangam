package com.apimarketplace.dto.kyc;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record KycVerificationSummary(
    List<KycVerificationOutcome> outcomes
) {

    public boolean allVerified() {
        return outcomes != null && outcomes.stream().allMatch(KycVerificationOutcome::verified);
    }

    public String providerLabel() {
        return outcomes == null || outcomes.isEmpty()
            ? "LOCAL"
            : outcomes.stream()
                .map(KycVerificationOutcome::provider)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));
    }

    public String summaryText() {
        if (outcomes == null || outcomes.isEmpty()) {
            return "No verification methods were executed.";
        }
        return outcomes.stream()
            .map(outcome -> outcome.method().name() + "=" + (outcome.verified() ? "VERIFIED" : "FAILED") + " (" + safe(outcome.message()) + ")")
            .collect(Collectors.joining(" | "));
    }

    public String referenceText() {
        if (outcomes == null || outcomes.isEmpty()) {
            return null;
        }
        return outcomes.stream()
            .map(KycVerificationOutcome::referenceId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.joining(","));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "n/a" : value;
    }
}
