package com.smartCommunity.SmartCommunity.dashboard;

import com.smartCommunity.SmartCommunity.dashboard.DashboardDtos.DashboardResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Home page data. Needs:  Authorization: Bearer <accessToken>
    @GetMapping
    public DashboardResponse getDashboard(@AuthenticationPrincipal UserDetails principal) {
        return dashboardService.getDashboard(principal.getUsername());
    }
}
