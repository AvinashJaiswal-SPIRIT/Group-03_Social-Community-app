package com.smartCommunity.SmartCommunity.community;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "communities")
@Getter
@Setter
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    // Optional. Frontend shows a default icon when this is null.
    @Column(length = 500)
    private String iconUrl;

    // EnumType.STRING stores "PUBLIC"/"PRIVATE" in the DB (readable, and safe if the enum order changes)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityVisibility visibility;

    // Short random code (e.g. "K7P2QX9M") that people type/paste to join
    @Column(nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Community() {
    }

    public Community(String name,
                     String description,
                     String iconUrl,
                     CommunityVisibility visibility,
                     String inviteCode) {
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.visibility = visibility;
        this.inviteCode = inviteCode;
    }
}
