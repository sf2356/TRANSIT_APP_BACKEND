package com.transit.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * CORRECTIF AUDIT (Prompt 07 §41/§65) : Prompt 04 §11 exige explicitement une taille de
 * page maximale ("size <= 100", "ne jamais permettre size=1000000"), mais aucune limite
 * n'avait été configurée — Spring Data applique par défaut un maximum de 2000 éléments par
 * page, largement supérieur à la limite documentée et jamais vérifiée jusqu'ici. N'importe
 * quel endpoint paginé (`GET /dossiers?size=2000`, etc.) pouvait donc renvoyer des réponses
 * bien plus volumineuses que prévu. Ce correctif borne explicitement la taille à 100 et la
 * valeur par défaut à 20, conformément au contrat documenté.
 */
@Configuration
public class PaginationConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(100);
            resolver.setFallbackPageable(PageRequest.of(0, 20));
        };
    }
}
