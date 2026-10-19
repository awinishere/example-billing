package com.example.plans.applications.catalog.create;

public record CreatePlanCommand(
        String code,
        String name,
        String description) {
}
