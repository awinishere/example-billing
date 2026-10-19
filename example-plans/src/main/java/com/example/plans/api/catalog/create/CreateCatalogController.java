package com.example.plans.api.catalog.create;

import com.example.plans.applications.catalog.create.CreatePlanHandler;
import com.example.plans.applications.catalog.create.CreatePlanResult;
import jakarta.validation.Valid;
import java.net.URI;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@AllArgsConstructor
public class CreateCatalogController {

    private final CreatePlanHandler createPlanHandler;

    @PostMapping
    public ResponseEntity<CreatePlanResponse> create(
            @Valid
            @RequestBody
            CreatePlanRequest request
    ) {
        CreatePlanResult result = createPlanHandler.execute(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/v1/plans/" + result.id()))
                .body(CreatePlanResponse.from(result));
    }
}
