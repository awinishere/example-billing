package com.example.plans.api.catalog.update;

import com.example.plans.applications.catalog.update.UpdatePlanCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateCatalogRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull @Size(max = 2000) String description) {

    public UpdatePlanCommand toCommand(UUID id) {
        return new UpdatePlanCommand(id, name, description);
    }
}
