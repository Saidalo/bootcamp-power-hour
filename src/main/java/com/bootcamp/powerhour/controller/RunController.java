package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.entity.PlannedRunStatus;
import com.bootcamp.powerhour.service.CurrentUserService;
import com.bootcamp.powerhour.service.PlannedRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/runs")
@RequiredArgsConstructor
public class RunController {
    private final CurrentUserService currentUserService;
    private final PlannedRunService plannedRunService;

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("runs", plannedRunService.runsOf(currentUserService.get(principal).getId()));
        return "runs";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable long id, Principal principal) {
        plannedRunService.changeStatus(id, currentUserService.get(principal).getId(), PlannedRunStatus.CANCELED);
        return "redirect:/runs";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable long id, Principal principal) {
        plannedRunService.changeStatus(id, currentUserService.get(principal).getId(), PlannedRunStatus.COMPLETED);
        return "redirect:/runs";
    }
}