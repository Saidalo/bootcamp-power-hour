package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.PlannedRun;
import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks that every query on planned runs is scoped to the owner of the
 * appliance, and that the database refuses runs that make no sense,
 * against a real database rather than a mock.
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
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "elering.import-on-startup=false",
        "elering.import-cron=-"
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

    @Autowired
    private EntityManager entityManager;

    private User ana;
    private User boris;
    private Appliance anasDishwasher;
    private PlannedRun anasRun;
    private PlannedRun borissRun;

    @BeforeEach
    void setUp() {
        ana = saveUser("ana");
        boris = saveUser("boris");

        anasDishwasher = saveAppliance(ana, "Dishwasher");
        Appliance borissDryer = saveAppliance(boris, "Dryer");

        anasRun = saveRun(anasDishwasher, TONIGHT, PlannedRunStatus.PLANNED);
        borissRun = saveRun(borissDryer, TONIGHT, PlannedRunStatus.PLANNED);
    }

    // --- Listing runs, oldest first (the "My runs" page) ---

    @Test
    void listsOnlyTheRunsOfTheGivenUser() {
        List<PlannedRun> found = plannedRunRepository
                .findByApplianceUserIdOrderByStartsAtAsc(ana.getId());

        assertThat(found).extracting(PlannedRun::getId).containsExactly(anasRun.getId());
    }

    @Test
    void listsNothingForAUserWithoutRuns() {
        User clara = saveUser("clara");

        assertThat(plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(clara.getId())).isEmpty();
    }
    @Test
    void listsMyRunsOldestFirst() {
        PlannedRun later = saveRun(anasDishwasher, TONIGHT.plusDays(1), PlannedRunStatus.PLANNED);
        PlannedRun earlier = saveRun(anasDishwasher, TONIGHT.minusDays(1), PlannedRunStatus.COMPLETED);

        List<PlannedRun> found = plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(ana.getId());

        assertThat(found).extracting(PlannedRun::getId)
                .containsExactly(earlier.getId(), anasRun.getId(), later.getId());
    }

    @Test
    void listsRunsOfAllMyAppliancesButNotOfOthers() {
        Appliance anasDryer = saveAppliance(ana, "Dryer");
        PlannedRun dryerRun = saveRun(anasDryer, TONIGHT.plusHours(3), PlannedRunStatus.PLANNED);

        List<PlannedRun> found = plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(ana.getId());

        assertThat(found).extracting(PlannedRun::getId)
                .containsExactly(anasRun.getId(), dryerRun.getId())
                .doesNotContain(borissRun.getId());
    }

    // --- Finding one run ---

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

    @Test
    void doesNotFindARunThatDoesNotExist() {
        assertThat(plannedRunRepository.findByIdAndApplianceUserId(999_999L, ana.getId())).isEmpty();
    }

    // --- The next planned run (home page) ---

    @Test
    void findsTheNextPlannedRun() {
        Optional<PlannedRun> next = plannedRunRepository
                .findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                        ana.getId(), PlannedRunStatus.PLANNED, TONIGHT.minusHours(1));

        assertThat(next).map(PlannedRun::getId).contains(anasRun.getId());
    }

    @Test
    void theNextRunIsTheEarliestOneAhead() {
        saveRun(anasDishwasher, TONIGHT.plusDays(2), PlannedRunStatus.PLANNED);
        PlannedRun tomorrow = saveRun(anasDishwasher, TONIGHT.plusDays(1), PlannedRunStatus.PLANNED);

        Optional<PlannedRun> next = plannedRunRepository
                .findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                        ana.getId(), PlannedRunStatus.PLANNED, TONIGHT.plusMinutes(1));

        assertThat(next).map(PlannedRun::getId).contains(tomorrow.getId());
    }

    @Test
    void aRunThatAlreadyStartedIsNotTheNextOne() {
        Optional<PlannedRun> next = plannedRunRepository
                .findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                        ana.getId(), PlannedRunStatus.PLANNED, TONIGHT);

        assertThat(next).isEmpty();
    }

    @Test
    void canceledAndCompletedRunsAreNotTheNextOne() {
        anasRun.setStatus(PlannedRunStatus.CANCELED);
        saveRun(anasDishwasher, TONIGHT.plusDays(1), PlannedRunStatus.COMPLETED);

        Optional<PlannedRun> next = plannedRunRepository
                .findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                        ana.getId(), PlannedRunStatus.PLANNED, TONIGHT.minusHours(1));

        assertThat(next).isEmpty();
    }

    @Test
    void someoneElsesEarlierRunIsNotMyNextOne() {
        borissRun.setStartsAt(TONIGHT.minusHours(2));
        borissRun.setEndsAt(TONIGHT.minusHours(1));

        Optional<PlannedRun> next = plannedRunRepository
                .findFirstByApplianceUserIdAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                        ana.getId(), PlannedRunStatus.PLANNED, TONIGHT.minusHours(3));

        assertThat(next).map(PlannedRun::getId).contains(anasRun.getId());
    }

    // --- Completed runs of a month (savings) ---

    @Test
    void theMonthIncludesItsFirstMomentAndExcludesTheNextMonth() {
        OffsetDateTime from = OffsetDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        PlannedRun first = saveRun(anasDishwasher, from, PlannedRunStatus.COMPLETED);
        saveRun(anasDishwasher, to, PlannedRunStatus.COMPLETED);
        saveRun(anasDishwasher, from.minusMinutes(1), PlannedRunStatus.COMPLETED);

        List<PlannedRun> found = plannedRunRepository
                .findByApplianceUserIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                        ana.getId(), PlannedRunStatus.COMPLETED, from, to);

        assertThat(found).extracting(PlannedRun::getId).containsExactly(first.getId());
    }

    @Test
    void theMonthOnlyHasRunsWithTheGivenStatus() {
        OffsetDateTime from = OffsetDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        PlannedRun completed = saveRun(anasDishwasher, TONIGHT.minusDays(1), PlannedRunStatus.COMPLETED);
        saveRun(anasDishwasher, TONIGHT.minusDays(2), PlannedRunStatus.CANCELED);

        List<PlannedRun> found = plannedRunRepository
                .findByApplianceUserIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                        ana.getId(), PlannedRunStatus.COMPLETED, from, to);

        // anasRun (planned) and the canceled run are left out
        assertThat(found).extracting(PlannedRun::getId).containsExactly(completed.getId());
    }

    @Test
    void theMonthOnlyHasMyRuns() {
        OffsetDateTime from = OffsetDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        borissRun.setStatus(PlannedRunStatus.COMPLETED);

        List<PlannedRun> found = plannedRunRepository
                .findByApplianceUserIdAndStatusAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                        ana.getId(), PlannedRunStatus.COMPLETED, from, to);

        assertThat(found).isEmpty();
    }


    // --- History is kept when an appliance is deleted (soft delete) ---

    @Test
    void keepsTheRunsOfADeletedAppliance() {
        anasDishwasher.setActive(false);
        entityManager.flush();
        entityManager.clear();

        assertThat(plannedRunRepository.findByApplianceUserIdOrderByStartsAtAsc(ana.getId()))
                .extracting(PlannedRun::getId).containsExactly(anasRun.getId());
    }

    @Test
    void aRunKeepsItsEnergyAndZoneWhenTheApplianceIsEdited() {
        anasDishwasher.setPowerKw(new BigDecimal("3.50"));
        anasDishwasher.setDurationMinutes(200);
        anasDishwasher.setBiddingZone(BiddingZone.EE);
        entityManager.flush();
        entityManager.clear();

        PlannedRun found = plannedRunRepository.findByIdAndApplianceUserId(anasRun.getId(), ana.getId()).orElseThrow();

        assertThat(found).isNotSameAs(anasRun);
        assertThat(found.getEnergyKwh()).isEqualByComparingTo("4.0000");
        assertThat(found.getBiddingZone()).isEqualTo(BiddingZone.LV);
        assertThat(found.getEstimatedCost()).isEqualByComparingTo("0.400000");
    }

    // --- The database refuses runs that make no sense ---

    @Test
    void refusesARunThatEndsBeforeItStarts() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEndsAt(TONIGHT.minusMinutes(1));

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesARunThatEndsTheMomentItStarts() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEndsAt(TONIGHT);

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesZeroEnergy() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEnergyKwh(BigDecimal.ZERO);

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesNegativeEnergy() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEnergyKwh(new BigDecimal("-1.0000"));

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesARunWithoutEnergy() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEnergyKwh(null);

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesARunWithoutAZone() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setBiddingZone(null);

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesARunWithoutACost() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEstimatedCost(null);

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesARunWithoutAStatus() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setStatus(null);

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesARunWithoutAnAppliance() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setAppliance(null);

        assertThatThrownBy(() -> plannedRunRepository.saveAndFlush(run))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptsTheSmallestPositiveEnergy() {
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEnergyKwh(new BigDecimal("0.0001"));

        PlannedRun saved = plannedRunRepository.saveAndFlush(run);

        assertThat(saved.getId()).isPositive();
    }

    @Test
    void acceptsAFreeOrNegativeCost() {
        // negative day-ahead prices happen, so a run can earn money
        PlannedRun run = newRun(anasDishwasher, TONIGHT);
        run.setEstimatedCost(new BigDecimal("-0.120000"));

        PlannedRun saved = plannedRunRepository.saveAndFlush(run);

        assertThat(saved.getEstimatedCost()).isEqualByComparingTo("-0.12");
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

    private PlannedRun saveRun(Appliance appliance, OffsetDateTime startsAt, PlannedRunStatus status) {
        PlannedRun run = newRun(appliance, startsAt);
        run.setStatus(status);
        return plannedRunRepository.save(run);
    }

    private PlannedRun newRun(Appliance appliance, OffsetDateTime startsAt) {
        PlannedRun run = new PlannedRun();
        run.setAppliance(appliance);
        run.setBiddingZone(appliance.getBiddingZone());
        run.setStartsAt(startsAt);
        run.setEndsAt(startsAt.plusMinutes(appliance.getDurationMinutes()));
        run.setEnergyKwh(new BigDecimal("4.0000"));
        run.setEstimatedCost(new BigDecimal("0.400000"));
        run.setStatus(PlannedRunStatus.PLANNED);
        return run;
    }
}