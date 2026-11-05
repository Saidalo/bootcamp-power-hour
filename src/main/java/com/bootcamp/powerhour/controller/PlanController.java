package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.dto.CheapestWindow;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.NoWindowAvailableException;
import com.bootcamp.powerhour.exception.PricesNotAvailableException;
import com.bootcamp.powerhour.service.ApplianceService;
import com.bootcamp.powerhour.service.CheapestWindowService;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.PlannedRunService;
import com.bootcamp.powerhour.service.PowerHourTime;
import com.bootcamp.powerhour.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PlanController {
    private final CurrentUserService currentUserService;
    private final ApplianceService applianceService;
    private final PriceService priceService;
    private final CheapestWindowService cheapestWindowService;
    private final PlannedRunService plannedRunService;

    @GetMapping("/plan")
    public String plan(@RequestParam(required = false) Long applianceId,
                       @RequestParam(defaultValue = "today") String day,
                       Principal principal, Model model) {
        User user = currentUserService.get(principal);
        model.addAttribute("appliances", applianceService.getAppliancesByUserId(user.getId()));
        model.addAttribute("day", day);
        model.addAttribute("selectedId", applianceId);
        if (applianceId == null) {
            return "plan";
        }

        Appliance appliance = applianceService.getOwned(applianceId, user.getId());
        LocalDate date = dateOf(day);
        List<ElectricityPrice> prices = priceService.pricesForDay(appliance.getBiddingZone(), date);
        if (prices.isEmpty()) {
            model.addAttribute("error", date.isAfter(PowerHourTime.today())
                    ? "Tomorrow's prices aren't published yet. Try again after 14:00."
                    : "Today's prices aren't imported yet.");
            return "plan";
        }

        OffsetDateTime searchFrom = searchFrom(date);
        boolean[] allowed = new boolean[prices.size()];
        boolean[] chosen = new boolean[prices.size()];
        for (int i = 0; i < prices.size(); i++) {
            OffsetDateTime start = prices.get(i).getStartsAt();
            allowed[i] = !start.isBefore(searchFrom) && CheapestWindowService.isAllowedTime(
                    start.atZoneSameInstant(PowerHourTime.ZONE).toLocalTime(),
                    appliance.getAllowedFrom(), appliance.getAllowedTo());
        }

        try {
            CheapestWindow window = findWindow(appliance, date);
            for (int i = 0; i < prices.size(); i++) {
                OffsetDateTime start = prices.get(i).getStartsAt();
                chosen[i] = !start.isBefore(window.startsAt()) && start.isBefore(window.endsAt());
            }
            model.addAttribute("window", window);
            model.addAttribute("startLabel", PowerHourTime.time(window.startsAt()));
            model.addAttribute("endLabel", PowerHourTime.time(window.endsAt()));
            model.addAttribute("costLabel", window.estimatedCost().setScale(2, RoundingMode.HALF_UP));
            model.addAttribute("energyLabel", window.energyKwh().setScale(2, RoundingMode.HALF_UP));
            model.addAttribute("durationLabel", PowerHourTime.durationLabel(appliance.getDurationMinutes()));
        } catch (NoWindowAvailableException e) {
            model.addAttribute("error", fitsSomewhereThatDay(appliance, date)
                    ? "Today's allowed window has already passed. Try Tomorrow."
                    : "This programme is longer than the window you allow. Widen it, or shorten the run.");
        } catch (PricesNotAvailableException e) {
            model.addAttribute("error", "Not enough prices left today. Try Tomorrow.");
        }
        model.addAttribute("slots", priceService.toSlots(prices, allowed, chosen));
        return "plan";
    }

    /** The browser only says which appliance and day; the window and cost are recalculated here. */
    @PostMapping("/plan")
    public String save(@RequestParam Long applianceId, @RequestParam String day, Principal principal) {
        User user = currentUserService.get(principal);
        Appliance appliance = applianceService.getOwned(applianceId, user.getId());
        plannedRunService.plan(appliance, findWindow(appliance, dateOf(day)));
        return "redirect:/runs";
    }

    private CheapestWindow findWindow(Appliance appliance, LocalDate date) {
        return cheapestWindowService.findCheapestWindow(appliance, searchFrom(date), endOf(date));
    }

    private boolean fitsSomewhereThatDay(Appliance appliance, LocalDate date) {
        try {
            cheapestWindowService.findCheapestWindow(appliance, PowerHourTime.startOfDay(date), endOf(date));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** For today, only slots that haven't started yet. */
    private static OffsetDateTime searchFrom(LocalDate date) {
        OffsetDateTime dayStart = PowerHourTime.startOfDay(date);
        OffsetDateTime now = OffsetDateTime.now();
        return now.isAfter(dayStart) ? now : dayStart;
    }

    /** findByBiddingZoneAndStartsAtBetween includes the end, so stop one second before midnight. */
    private static OffsetDateTime endOf(LocalDate date) {
        return PowerHourTime.startOfDay(date.plusDays(1)).minusSeconds(1);
    }

    private static LocalDate dateOf(String day) {
        return "tomorrow".equals(day) ? PowerHourTime.today().plusDays(1) : PowerHourTime.today();
    }
}