package com.smartCommunity.SmartCommunity.community;

import com.smartCommunity.SmartCommunity.common.PageResponse;
import com.smartCommunity.SmartCommunity.community.CommunityDtos.CommunityResponse;
import com.smartCommunity.SmartCommunity.community.CommunityDtos.CreateCommunityRequest;
import com.smartCommunity.SmartCommunity.users.AppUser;
import com.smartCommunity.SmartCommunity.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create a community and read communities (details, "my" list, "discover" list).
 * Joining / leaving lives in MembershipService.
 *
 * Every public method takes the logged-in user's EMAIL (taken from the JWT by the controller).
 */
@Service
public class CommunityService {

    // No 0/O/1/I/L so codes are easy to read out loud
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_PAGE_SIZE = 50;

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public CommunityService(CommunityRepository communityRepository,
                            CommunityMemberRepository memberRepository,
                            UserRepository userRepository) {
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------------- create

    @Transactional
    public CommunityResponse create(String email, CreateCommunityRequest request) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        String name = request.name().trim();
        if (name.length() < 3) { // @Size checks the untrimmed text, so check again after trim()
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Community name must be at least 3 characters");
        }
        if (communityRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A community with this name already exists");
        }

        Community community = communityRepository.save(new Community(
                name,
                blankToNull(request.description()),
                blankToNull(request.iconUrl()),
                request.visibility(),
                generateUniqueInviteCode()));

        // The creator automatically becomes the OWNER (this is also what makes it appear in "My Communities")
        memberRepository.save(new CommunityMember(community, user, CommunityRole.OWNER));

        return toResponse(community, user);
    }

    // ---------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public CommunityResponse getById(String email, Long communityId) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        Community community = communityRepository.findById(communityId)
                .orElseThrow(this::notFound);

        // A private community is invisible (404) to people who are not inside it
        boolean isMember = memberRepository
                .existsByCommunityIdAndUserId(communityId, user.getId());
        if (community.getVisibility() == CommunityVisibility.PRIVATE && !isMember) {
            throw notFound();
        }

        return toResponse(community, user);
    }

    // "My Communities": everything the user has joined (including ones they own)
    @Transactional(readOnly = true)
    public PageResponse<CommunityResponse> getMyCommunities(String email, int page, int size) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        Page<Community> result = memberRepository
                .findCommunitiesOfUser(user.getId(), pageable(page, size));

        return PageResponse.of(toResponses(result.getContent(), user), result);
    }

    // "Discover Communities": public communities the user has not joined. "search" is optional.
    @Transactional(readOnly = true)
    public PageResponse<CommunityResponse> discover(String email, String search, int page, int size) {

        AppUser user = userRepository.getByEmailOrThrow(email);

        // Always pass a non-null pattern ("%%" matches everything) - avoids NULL problems in PostgreSQL
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";

        Page<Community> result = communityRepository.findDiscoverable(
                user.getId(), CommunityVisibility.PUBLIC, pattern, pageable(page, size));

        return PageResponse.of(toResponses(result.getContent(), user), result);
    }

    // ---------------------------------------------------------------- mapping (entity -> JSON)

    public CommunityResponse toResponse(Community community, AppUser user) {
        return toResponses(List.of(community), user).get(0);
    }

    /**
     * Converts a whole list using only 2 extra queries (member counts + my roles),
     * instead of 2 queries for every community card.
     */
    private List<CommunityResponse> toResponses(List<Community> communities, AppUser user) {

        if (communities.isEmpty()) {
            return List.of();
        }

        List<Long> ids = communities.stream().map(Community::getId).toList();

        Map<Long, Long> memberCounts = new HashMap<>();
        for (CommunityMemberRepository.MemberCount row : memberRepository.countMembersByCommunityIds(ids)) {
            memberCounts.put(row.getCommunityId(), row.getTotal());
        }

        Map<Long, CommunityRole> myRoles = new HashMap<>();
        for (CommunityMember member : memberRepository.findByUserIdAndCommunityIdIn(user.getId(), ids)) {
            myRoles.put(member.getCommunity().getId(), member.getRole());
        }

        return communities.stream().map(community -> {
            CommunityRole myRole = myRoles.get(community.getId()); // null = not a member

            // Only the owner / moderators are allowed to see the invite code
            boolean canSeeCode = myRole == CommunityRole.OWNER || myRole == CommunityRole.MODERATOR;

            return new CommunityResponse(
                    community.getId(),
                    community.getName(),
                    community.getDescription(),
                    community.getIconUrl(),
                    community.getVisibility(),
                    memberCounts.getOrDefault(community.getId(), 0L),
                    myRole,
                    canSeeCode ? community.getInviteCode() : null,
                    community.getCreatedAt());
        }).toList();
    }

    // ---------------------------------------------------------------- helpers

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = builder.toString();
        } while (communityRepository.existsByInviteCode(code)); // practically never repeats
        return code;
    }

    // page is 0-based; size is limited so nobody can ask for 1,000,000 rows at once
    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found");
    }
}
