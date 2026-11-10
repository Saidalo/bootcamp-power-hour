package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that every query on planned runs is scoped to the owner of the
 * appliance, against a real database rather than a mock.
 *
 * Runs on an in-memory database, so it needs no Docker and leaves the
 * development data alone. The whole test rolls back afterwards.
 *
 * @author Karyna Litvinova
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:planned_run_repository_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PlannedRunRepositoryTest {

    private static final OffsetDateTime TONIGHT =
            OffsetDateTime.of(2026, 11, 6, 22, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplianceRepository applianceRepository;

    @Autowired
    private PlannedRunRepository plannedRunRepository;

    private User ana;
    private PlannedRun anasRun;
    private PlannedRun borissRun;

    @BeforeEach
    void setUp() {
        ana = saveUser("ana");
        User boris = saveUser("boris");

        Appliance anasDishwasher = saveAppliance(ana, "Dishwasher");
        Appliance borissDryer = saveAppliance(boris, "Dryer");

        anasRun = saveRun(anasDishwasher, TONIGHT);
        borissRun = saveRun(borissDryer, TONIGHT);
    }

    @Test
    void listsOnlyTheRunsOfTheGivenUser() {
        List<PlannedRun> found = plannedRunRepository
                .findByApplianceUserIdOrderByStartsAtDesc(ana.getId());

        assertThat(found).extracting(PlannedRun::getId).containsExactly(anasRun.getId());
    }

    @Test
    void returnsTheLatestRunFirst() {
        Appliance anasDryer = saveAppliance(ana, "Dryer");
        PlannedRun later = saveRun(anasDryer, TONIGHT.plusDays(1));

        List<PlannedRun> found = plannedRunRepository
                .findByApplianceUserIdOrderByStartsAtDesc(ana.getId());

        assertThat(found).extracting(PlannedRun::getId)
                .containsExactly(later.getId(), anasRun.getId());
    }

    @Test
    void findsARunOfItsOwner() {
        Optional<PlannedRun> found = plannedRunRepository
                .findByIdAndApplianceUserId(anasRun.getId(), ana.getId());

        assertThat(found).isPresent();
    }

    @Test
    void doesNotFindSomeoneElsesRunEvenWithTheRightId() {
        Optional<PlannedRun> found = plannedRunRepository
                .findByIdAndApplianceUserId(borissRun.getId(), ana.getId());

        assertThat(found).isEmpty();
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

    private PlannedRun saveRun(Appliance appliance, OffsetDateTime startsAt) {
        PlannedRun run = new PlannedRun();
        run.setAppliance(appliance);
        run.setBiddingZone(appliance.getBiddingZone());
        run.setStartsAt(startsAt);
        run.setEndsAt(startsAt.plusMinutes(appliance.getDurationMinutes()));
        run.setEnergyKwh(new BigDecimal("4.0000"));
        run.setEstimatedCost(new BigDecimal("0.400000"));
        run.setStatus(PlannedRunStatus.PLANNED);
        return plannedRunRepository.save(run);
    }
}