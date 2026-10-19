package com.example.plans.api.catalog.update;

import com.example.plans.applications.catalog.update.UpdatePlanHandler;
import com.example.plans.applications.catalog.update.UpdatePlanResult;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@AllArgsConstructor
public class UpdateCatalogController {

    private final UpdatePlanHandler updatePlanHandler;

    @PutMapping("/{id}")
    public ResponseEntity<UpdateCatalogResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCatalogRequest request
    ) {
        UpdatePlanResult result = updatePlanHandler.execute(request.toCommand(id));
        return ResponseEntity.ok(UpdateCatalogResponse.from(result));
    }
}
