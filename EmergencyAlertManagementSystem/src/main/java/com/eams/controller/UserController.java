package com.eams.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.eams.model.User;
import com.eams.repository.UserRepository;
import com.eams.service.AlertService;
import com.eams.service.UserService;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserRepository userRepository; 

    @GetMapping("/")
    public String index() {
        return "index"; 
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("success", model.containsAttribute("success") ? model.asMap().get("success") : null);
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            user.setRole("USER"); // Default role
            userService.registerUser(user);
            model.addAttribute("success", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        if ("USER".equals(user.getRole())) {
            model.addAttribute("alerts", alertService.getUserAlerts(user));
            return "user_dashboard";
        } else if ("RESPONDER".equals(user.getRole())) {
            model.addAttribute("alerts", alertService.getResponderAlerts(user));
            return "responder_dashboard";
        } else if ("ADMIN".equals(user.getRole())) {
            model.addAttribute("alerts", alertService.getAllAlerts());
            model.addAttribute("responders", userRepository.findAll().stream()
                .filter(u -> "RESPONDER".equals(u.getRole()) && u.isAvailable()).toList());
            return "admin_dashboard";
        }
        return "redirect:/login";
    }
}
