package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.PlannedRunService;
import com.bootcamp.powerhour.service.PowerHourTime;
import com.bootcamp.powerhour.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final CurrentUserService currentUserService;
    private final PriceService priceService;
    private final PlannedRunService plannedRunService;

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        User user = currentUserService.get(principal);
        List<ElectricityPrice> prices = priceService.pricesForDay(CurrentUserService.zoneOf(user), PowerHourTime.today());

        priceService.cheapest(prices).ifPresent(cheapest -> {
            model.addAttribute("cheapestTime", PowerHourTime.time(cheapest.getStartsAt()));
            model.addAttribute("cheapestPrice", cheapest.getPriceEurKwh().setScale(4, java.math.RoundingMode.HALF_UP));
        });
        model.addAttribute("slots", priceService.toSlots(prices, null, null));
        model.addAttribute("nextRun", plannedRunService.nextPlanned(user.getId()).orElse(null));
        return "index";
    }
}