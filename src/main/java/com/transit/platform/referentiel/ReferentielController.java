package com.transit.platform.referentiel;

import com.transit.platform.common.ApiResponse;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/referentiels")
@Tag(name = "Référentiels", description = "Valeurs métier pour les listes déroulantes — source unique pour Angular et Flutter")
public class ReferentielController {

    @GetMapping
    @Operation(summary = "Lister les clés de référentiel disponibles")
    public ApiResponse<Set<String>> keys() {
        return ApiResponse.of(ReferentielCatalogue.CATALOGUE.keySet());
    }

    @GetMapping("/{key}")
    @Operation(summary = "Récupérer un référentiel (ex. modes-transport, incoterms, types-charge...)")
    public ApiResponse<List<ReferentielItem>> get(@PathVariable String key) {
        List<ReferentielItem> items = ReferentielCatalogue.CATALOGUE.get(key);
        if (items == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Référentiel inconnu : " + key, HttpStatus.NOT_FOUND);
        }
        return ApiResponse.of(items);
    }
}
