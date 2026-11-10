package com.bootcamp.powerhour.controller;

import com.bootcamp.powerhour.dto.RegisterRequest;
import com.bootcamp.powerhour.exception.UserAlreadyExistsException;
import com.bootcamp.powerhour.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistrationController {
    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("form", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(form);
        } catch (UserAlreadyExistsException ex) {
            bindingResult.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "register";
        }

        return "redirect:/login?registered";
    }
}