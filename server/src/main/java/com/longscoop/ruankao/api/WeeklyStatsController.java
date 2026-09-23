package com.longscoop.ruankao.api;

import com.longscoop.ruankao.analytics.WeeklyStatsService;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/stats")
public class WeeklyStatsController {

    private final WeeklyStatsService weeklyStatsService;

    public WeeklyStatsController(WeeklyStatsService weeklyStatsService) {
        this.weeklyStatsService = weeklyStatsService;
    }

    @GetMapping("/weekly")
    public WeeklyStatsService.WeeklyStats weekly(
            @AuthenticationPrincipal RuankaoPrincipal principal) {
        return weeklyStatsService.get(principal.userId());
    }
}
