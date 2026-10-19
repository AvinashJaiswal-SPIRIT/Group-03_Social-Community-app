package com.smartCommunity.SmartCommunity.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByInviteCode(String inviteCode);

    Optional<Community> findByInviteCode(String inviteCode);

    /**
     * Communities to show in "Discover": public ones the user has NOT joined yet.
     * "pattern" is already lowercase and wrapped in % (e.g. "%java%"); use "%%" to match everything.
     */
    @Query("""
            select c from Community c
            where c.visibility = :visibility
              and lower(c.name) like :pattern
              and not exists (
                    select m.id from CommunityMember m
                    where m.community = c and m.user.id = :userId)
            order by c.createdAt desc
            """)
    Page<Community> findDiscoverable(@Param("userId") Long userId,
                                     @Param("visibility") CommunityVisibility visibility,
                                     @Param("pattern") String pattern,
                                     Pageable pageable);
}
