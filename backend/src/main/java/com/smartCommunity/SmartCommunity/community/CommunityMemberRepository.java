package com.smartCommunity.SmartCommunity.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    boolean existsByCommunityIdAndUserId(Long communityId, Long userId);

    Optional<CommunityMember> findByCommunityIdAndUserId(Long communityId, Long userId);

    // The user's membership rows for a batch of communities (used to fill "myRole" on cards)
    List<CommunityMember> findByUserIdAndCommunityIdIn(Long userId, Collection<Long> communityIds);

    // "My Communities": the communities this user is in, newest join first
    @Query(value = """
            select m.community from CommunityMember m
            where m.user.id = :userId
            order by m.joinedAt desc
            """,
            countQuery = "select count(m) from CommunityMember m where m.user.id = :userId")
    Page<Community> findCommunitiesOfUser(@Param("userId") Long userId, Pageable pageable);

    // Member count for many communities in ONE query (instead of one query per card)
    @Query("""
            select m.community.id as communityId, count(m) as total
            from CommunityMember m
            where m.community.id in :communityIds
            group by m.community.id
            """)
    List<MemberCount> countMembersByCommunityIds(@Param("communityIds") Collection<Long> communityIds);

    // Spring fills this from the aliases (communityId, total) in the query above
    interface MemberCount {
        Long getCommunityId();

        Long getTotal();
    }
}
