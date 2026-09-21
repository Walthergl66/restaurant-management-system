package com.restaurante;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;


class ModularityTests {

    @Test
    void modularStructureIsVerified() {
        ApplicationModules modules = ApplicationModules.of(RestaurantApplication.class);
        modules.verify();
    }

    @Test
    void moduleStructureIsReported() {
        ApplicationModules modules = ApplicationModules.of(RestaurantApplication.class);
        modules.forEach(System.out::println);
    }
}