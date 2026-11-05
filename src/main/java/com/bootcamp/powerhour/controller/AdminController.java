package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.dto.ImportView;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.service.AdminUserService;
import com.bootcamp.powerhour.service.PowerHourTime;
import com.bootcamp.powerhour.service.PriceImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PriceImportService priceImportService;
    private final AdminUserService adminUserService;

    @GetMapping
    public String admin(Model model) {
        model.addAttribute("imports", priceImportService.history().stream()
                .map(i -> new ImportView(PowerHourTime.dayTime(i.getStartedAt()),
                        i.getBiddingZone() == null ? "–" : i.getBiddingZone() + " · " + i.getPriceDate(),
                        i.getRecordsImported(), i.getStatus(), i.getErrorMessage()))
                .toList());
        model.addAttribute("users", adminUserService.allUsers());
        model.addAttribute("zones", BiddingZone.values());
        model.addAttribute("today", PowerHourTime.today());
        return "admin";
    }

    @PostMapping("/imports")
    public String reimport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam BiddingZone zone) {
        priceImportService.importDay(zone, date);
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivate(@PathVariable long id, Principal principal, RedirectAttributes redirect) {
        try {
            adminUserService.deactivate(id, principal.getName());
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}