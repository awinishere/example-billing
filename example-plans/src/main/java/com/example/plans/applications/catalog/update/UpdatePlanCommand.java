package com.example.plans.applications.catalog.update;

import java.util.UUID;

public record UpdatePlanCommand(
        UUID id,
        String name,
        String description) {
}
