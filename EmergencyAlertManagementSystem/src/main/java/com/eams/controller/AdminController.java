package com.eams.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.eams.repository.UserRepository;
import com.eams.service.AlertService;

@Controller
public class AdminController {

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("alerts", alertService.getAllAlerts());
        model.addAttribute("responders", userRepository.findAll().stream()
            .filter(u -> "RESPONDER".equals(u.getRole()) && u.isAvailable()).toList()); // Filter available responders
        return "admin_dashboard";
    }

    @PostMapping("/admin/assign")
    public String assignAlert(@RequestParam("alertId") Long alertId, @RequestParam("responderId") Long responderId) {
        alertService.adminAssignAlert(alertId, responderId);
        return "redirect:/admin/dashboard";
    }
}