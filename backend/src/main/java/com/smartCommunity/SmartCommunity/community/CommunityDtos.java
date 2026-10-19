package com.smartCommunity.SmartCommunity.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTOs = the exact JSON going in/out of the API (we never expose entities directly).
 * Same style as AuthDtos: one holder class with small records inside.
 */
public final class CommunityDtos {

    private CommunityDtos() {
    }

    // POST /api/communities
    public record CreateCommunityRequest(
            @NotBlank
            @Size(min = 3, max = 50)
            String name,

            @Size(max = 255)
            String description,

            @Size(max = 500)
            String iconUrl,

            @NotNull
            CommunityVisibility visibility
    ) {
    }

    // POST /api/communities/join
    public record JoinByCodeRequest(
            @NotBlank
            @Size(max = 12)
            String inviteCode
    ) {
    }

    /**
     * One community card. The SAME shape is used everywhere
     * (dashboard, my list, discover list, details, create, join), so the frontend needs one component.
     *
     * myRole    -> OWNER / MODERATOR / MEMBER, or null if the current user has NOT joined.
     * inviteCode-> only filled for OWNER and MODERATOR, null for everyone else.
     */
    public record CommunityResponse(
            Long id,
            String name,
            String description,
            String iconUrl,
            CommunityVisibility visibility,
            long memberCount,
            CommunityRole myRole,
            String inviteCode,
            Instant createdAt
    ) {
    }
}
