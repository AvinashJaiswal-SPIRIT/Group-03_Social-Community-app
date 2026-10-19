package com.smartCommunity.SmartCommunity.community;

import com.smartCommunity.SmartCommunity.users.AppUser;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * One row = "this user is in this community with this role".
 * It is the link table between app_users and communities (many-to-many with extra data: role, joinedAt).
 */
@Entity
@Table(
        name = "community_members",
        // The same user can never be added to the same community twice
        uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "user_id"})
)
@Getter
public class CommunityMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityRole role;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    protected CommunityMember() {
    }

    public CommunityMember(Community community, AppUser user, CommunityRole role) {
        this.community = community;
        this.user = user;
        this.role = role;
    }
}
