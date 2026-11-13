package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.dto.ApplianceForm;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.service.ApplianceService;
import com.bootcamp.powerhour.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/appliances")
@RequiredArgsConstructor
public class ApplianceController {
    private final ApplianceService applianceService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String list(Principal principal, Model model) {
        User user = currentUserService.get(principal);
        model.addAttribute("appliances", applianceService.getAppliancesByUserId(user.getId()));
        return "appliances";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ApplianceForm());
        model.addAttribute("applianceId", null);
        return "appliance-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model) {
        User user = currentUserService.get(principal);
        model.addAttribute("form", applianceService.toForm(applianceService.getOwned(id, user.getId())));
        model.addAttribute("applianceId", id);
        return "appliance-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ApplianceForm form, BindingResult errors,
                         Principal principal, Model model) {
        return save(null, form, errors, principal, model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ApplianceForm form,
                         BindingResult errors, Principal principal, Model model) {
        return save(id, form, errors, principal, model);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal) {
        applianceService.delete(id, currentUserService.get(principal).getId());
        return "redirect:/appliances";
    }

    private String save(Long id, ApplianceForm form, BindingResult errors, Principal principal, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("applianceId", id);
            return "appliance-form";
        }
        applianceService.save(id, form, currentUserService.get(principal));
        return "redirect:/appliances";
    }
}