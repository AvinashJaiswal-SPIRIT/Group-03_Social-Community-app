package com.smartCommunity.SmartCommunity.dashboard;

import com.smartCommunity.SmartCommunity.community.CommunityDtos.CommunityResponse;

import java.util.List;

/**
 * JSON for GET /api/dashboard - everything the Home page needs in ONE call.
 * (Settings and Recent Activity are intentionally not included.)
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardResponse(
            UserSummary user,                          // "Good morning, Tarun!" + sidebar profile
            Stats stats,                               // the 4 cards under the greeting
            List<CommunityResponse> myCommunities,     // "My Communities" row (first few)
            List<CommunityResponse> discoverCommunities, // "Discover Communities" row (first few)
            CommunityStats communityStats              // "Community Stats" panel
    ) {
    }

    public record UserSummary(
            Long id,
            String displayName,
            String email
    ) {
    }

    // The four stat cards.
    // unreadMessages and newNotifications are 0 for now - they will be real numbers
    // once the messaging and notification modules exist (the field names will not change).
    public record Stats(
            long joinedCommunities,
            long availableCommunities,
            long unreadMessages,
            long newNotifications
    ) {
    }

    // "Community Stats" panel on the right
    public record CommunityStats(
            long totalCommunities,
            long totalMembers,   // total memberships across all communities
            String yourRole      // platform role of the user: "USER" or "ADMIN"
    ) {
    }
}
