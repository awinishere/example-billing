package com.example.plans.api.catalog.create;

import com.example.plans.applications.catalog.create.CreatePlanCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlanRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        @NotNull @Size(max = 2000) String description) {

    public CreatePlanCommand toCommand() {
        return new CreatePlanCommand(code, name, description);
    }
}
