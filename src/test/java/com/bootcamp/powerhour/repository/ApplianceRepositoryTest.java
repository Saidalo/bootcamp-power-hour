package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:planned_run_repository_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class ApplianceRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplianceRepository applianceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllByUserIdAndIsActiveTrue_shouldReturnOnlyActiveOwnedAppliances() {
        User ana = saveUser("ana");
        User boris = saveUser("boris");

        Appliance active = saveAppliance(ana, "Dishwasher");
        Appliance inactive = saveAppliance(ana, "Washing machine");
        inactive.setActive(false);
        saveAppliance(boris, "Dryer");

        entityManager.flush();

        List<Appliance> result = applianceRepository.findAllByUserIdAndIsActiveTrue(ana.getId());

        assertThat(result)
                .extracting(Appliance::getId)
                .containsExactly(active.getId());
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@powerhour.lv");
        user.setPasswordHash("hash");
        user.setRole(Role.USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    private Appliance saveAppliance(User owner, String name) {
        Appliance appliance = new Appliance();
        appliance.setUser(owner);
        appliance.setBiddingZone(BiddingZone.LV);
        appliance.setName(name);
        appliance.setPowerKw(new BigDecimal("2.00"));
        appliance.setDurationMinutes(120);
        appliance.setAllowedFrom(LocalTime.of(0, 0));
        appliance.setAllowedTo(LocalTime.of(7, 0));
        appliance.setActive(true);
        return applianceRepository.save(appliance);
    }
}
