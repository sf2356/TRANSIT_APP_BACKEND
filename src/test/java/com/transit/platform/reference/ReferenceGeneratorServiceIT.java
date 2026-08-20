package com.transit.platform.reference;

import com.transit.platform.entreprise.Entreprise;
import com.transit.platform.entreprise.EntrepriseRepository;
import com.transit.platform.entreprise.ParametreEntreprise;
import com.transit.platform.entreprise.ParametreEntrepriseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cas critique n°2 du Prompt 03 §47 : deux créations simultanées de dossiers dans la même
 * entreprise ne doivent JAMAIS recevoir le même numéro. On simule ici 20 générateurs
 * concurrents et on vérifie l'unicité stricte des numéros obtenus.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReferenceGeneratorServiceIT {

    @Autowired
    private ReferenceGeneratorService referenceGeneratorService;
    @Autowired
    private EntrepriseRepository entrepriseRepository;
    @Autowired
    private ParametreEntrepriseRepository parametreRepository;

    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        Entreprise entreprise = new Entreprise();
        entreprise.setNom("Entreprise Test Concurrence");
        entreprise.setEmail("concurrence-" + UUID.randomUUID() + "@test.local");
        entreprise.setDeviseDefaut("XOF");
        entreprise.setStatut("ACTIF");
        entreprise = entrepriseRepository.save(entreprise);
        entrepriseId = entreprise.getId();

        ParametreEntreprise parametres = new ParametreEntreprise();
        parametres.setEntrepriseId(entrepriseId);
        parametres.setDevise("XOF");
        parametres.setConfigMetier(Map.of());
        parametreRepository.save(parametres);
    }

    @Test
    void deuxUtilisateursCreantSimultanementUnDossier_recoiventDeuxNumerosDistincts() throws InterruptedException {
        int nbConcurrents = 20;
        ExecutorService executor = Executors.newFixedThreadPool(nbConcurrents);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<String>> futures = IntStream.range(0, nbConcurrents)
                .mapToObj(i -> executor.submit(() -> {
                    startLatch.await();
                    return referenceGeneratorService.generate(entrepriseId, ReferenceType.DOSSIER);
                }))
                .collect(Collectors.toList());

        startLatch.countDown();
        List<String> numeros = futures.stream().map(f -> {
            try {
                return f.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        executor.shutdown();

        assertThat(numeros).hasSize(nbConcurrents);
        assertThat(numeros).doesNotHaveDuplicates();
    }
}
