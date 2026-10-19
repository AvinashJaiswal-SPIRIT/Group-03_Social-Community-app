package com.smartCommunity.SmartCommunity.community;

/**
 * A user's role INSIDE one community (not the same as the platform-wide UserRole).
 * The same person can be OWNER of one community and just a MEMBER of another.
 */
public enum CommunityRole {
    OWNER,      // created the community
    MODERATOR,  // will review flagged messages later (moderation phase)
    MEMBER
}
