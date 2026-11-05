package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.PowerHourTime;
import com.bootcamp.powerhour.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class SavingsController {
    private final CurrentUserService currentUserService;
    private final SavingsService savingsService;

    @GetMapping("/savings")
    public String savings(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                          Principal principal, Model model) {
        YearMonth selected = month != null ? month : YearMonth.from(PowerHourTime.today());
        model.addAttribute("month", selected);
        model.addAttribute("summary", savingsService.forMonth(currentUserService.get(principal).getId(), selected));
        return "savings";
    }
}