package com.smartCommunity.SmartCommunity.community;

import com.smartCommunity.SmartCommunity.community.CommunityDtos.CommunityResponse;
import com.smartCommunity.SmartCommunity.users.AppUser;
import com.smartCommunity.SmartCommunity.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Joining and leaving communities.
 *
 * Two ways to join:
 *  1. join(...)        - click "Join" on a PUBLIC community card
 *  2. joinByCode(...)  - type an invite code (works for PUBLIC and PRIVATE communities)
 */
@Service
public class MembershipService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CommunityService communityService;

    public MembershipService(CommunityRepository communityRepository,
                             CommunityMemberRepository memberRepository,
                             UserRepository userRepository,
                             CommunityService communityService) {
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.communityService = communityService;
    }

    @Transactional
    public CommunityResponse join(String email, Long communityId) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Community not found"));

        // Private communities can NOT be joined by id - only with the invite code
        if (community.getVisibility() == CommunityVisibility.PRIVATE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This community is private. Join using an invite code.");
        }

        return addMember(community, user);
    }

    @Transactional
    public CommunityResponse joinByCode(String email, String inviteCode) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        // Codes are stored in UPPERCASE, so accept "k7p2qx9m" typed in lowercase too
        Community community = communityRepository
                .findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid invite code"));

        return addMember(community, user);
    }

    @Transactional
    public void leave(String email, Long communityId) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        CommunityMember member = memberRepository
                .findByCommunityIdAndUserId(communityId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "You are not a member of this community"));

        // An ownerless community would be stuck forever, so the owner can't just walk out
        // (transfer ownership / delete community can be added later)
        if (member.getRole() == CommunityRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The owner cannot leave their own community");
        }

        memberRepository.delete(member);
    }

    // Shared by both join methods
    private CommunityResponse addMember(Community community, AppUser user) {

        if (memberRepository.existsByCommunityIdAndUserId(community.getId(), user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You are already a member of this community");
        }

        memberRepository.save(new CommunityMember(community, user, CommunityRole.MEMBER));

        // Return the updated card (memberCount + myRole now reflect the join)
        return communityService.toResponse(community, user);
    }
}
