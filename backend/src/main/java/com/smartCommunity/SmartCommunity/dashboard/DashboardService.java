package com.smartCommunity.SmartCommunity.dashboard;

import com.smartCommunity.SmartCommunity.common.PageResponse;
import com.smartCommunity.SmartCommunity.community.CommunityDtos.CommunityResponse;
import com.smartCommunity.SmartCommunity.community.CommunityMemberRepository;
import com.smartCommunity.SmartCommunity.community.CommunityRepository;
import com.smartCommunity.SmartCommunity.community.CommunityService;
import com.smartCommunity.SmartCommunity.dashboard.DashboardDtos.CommunityStats;
import com.smartCommunity.SmartCommunity.dashboard.DashboardDtos.DashboardResponse;
import com.smartCommunity.SmartCommunity.dashboard.DashboardDtos.Stats;
import com.smartCommunity.SmartCommunity.dashboard.DashboardDtos.UserSummary;
import com.smartCommunity.SmartCommunity.users.AppUser;
import com.smartCommunity.SmartCommunity.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    // The dashboard shows 3 cards per row; "View all" uses the paginated /api/communities/... endpoints
    private static final int PREVIEW_SIZE = 3;

    private final UserRepository userRepository;
    private final CommunityService communityService;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;

    public DashboardService(UserRepository userRepository,
                            CommunityService communityService,
                            CommunityRepository communityRepository,
                            CommunityMemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.communityService = communityService;
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String email) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        // Reuse the community service. Each result carries the first 3 cards AND the total count,
        // so one query gives us both the card list and the number for the stat card.
        PageResponse<CommunityResponse> mine =
                communityService.getMyCommunities(email, 0, PREVIEW_SIZE);
        PageResponse<CommunityResponse> discover =
                communityService.discover(email, null, 0, PREVIEW_SIZE);

        Stats stats = new Stats(
                mine.totalItems(),        // Joined Communities
                discover.totalItems(),    // Available Communities (public, not joined yet)
                0,                        // Unread Messages  - added with the messaging module
                0);                       // New Notifications - added with the notification module

        CommunityStats communityStats = new CommunityStats(
                communityRepository.count(),
                memberRepository.count(),
                user.getRole().name());

        return new DashboardResponse(
                new UserSummary(user.getId(), displayNameOf(user), user.getEmail()),
                stats,
                mine.items(),
                discover.items(),
                communityStats);
    }

    // Users created before display names were saved have none - fall back to the part before the "@"
    private String displayNameOf(AppUser user) {
        String name = user.getDisplayName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return user.getEmail().substring(0, user.getEmail().indexOf('@'));
    }
}
