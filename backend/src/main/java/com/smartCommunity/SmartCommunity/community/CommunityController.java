package com.smartCommunity.SmartCommunity.community;

import com.smartCommunity.SmartCommunity.common.PageResponse;
import com.smartCommunity.SmartCommunity.community.CommunityDtos.CommunityResponse;
import com.smartCommunity.SmartCommunity.community.CommunityDtos.CreateCommunityRequest;
import com.smartCommunity.SmartCommunity.community.CommunityDtos.JoinByCodeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * All endpoints need the header:  Authorization: Bearer <accessToken>
 * (SecurityConfig already blocks everything except /api/auth/** without a token.)
 *
 * @AuthenticationPrincipal gives us the logged-in user that JwtAuthenticationFilter put in the
 * security context; getUsername() is the user's email.
 */
@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    private final CommunityService communityService;
    private final MembershipService membershipService;

    public CommunityController(CommunityService communityService,
                               MembershipService membershipService) {
        this.communityService = communityService;
        this.membershipService = membershipService;
    }

    // Create a community (the creator becomes OWNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityResponse create(@AuthenticationPrincipal UserDetails principal,
                                    @Valid @RequestBody CreateCommunityRequest request) {
        return communityService.create(principal.getUsername(), request);
    }

    // "My Communities" -> View all
    @GetMapping("/my")
    public PageResponse<CommunityResponse> myCommunities(@AuthenticationPrincipal UserDetails principal,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return communityService.getMyCommunities(principal.getUsername(), page, size);
    }

    // "Discover Communities" -> View all, also used by the search box (?search=java)
    @GetMapping("/discover")
    public PageResponse<CommunityResponse> discover(@AuthenticationPrincipal UserDetails principal,
                                                    @RequestParam(required = false) String search,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return communityService.discover(principal.getUsername(), search, page, size);
    }

    // Single community details
    @GetMapping("/{id}")
    public CommunityResponse getById(@AuthenticationPrincipal UserDetails principal,
                                     @PathVariable Long id) {
        return communityService.getById(principal.getUsername(), id);
    }

    // Join a PUBLIC community (the "Join" button on a discover card)
    @PostMapping("/{id}/join")
    public CommunityResponse join(@AuthenticationPrincipal UserDetails principal,
                                  @PathVariable Long id) {
        return membershipService.join(principal.getUsername(), id);
    }

    // Join with an invite code (the "Join Community" quick action) - works for PRIVATE ones too
    @PostMapping("/join")
    public CommunityResponse joinByCode(@AuthenticationPrincipal UserDetails principal,
                                        @Valid @RequestBody JoinByCodeRequest request) {
        return membershipService.joinByCode(principal.getUsername(), request.inviteCode());
    }

    // Leave a community (204 = success, no body)
    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal UserDetails principal,
                      @PathVariable Long id) {
        membershipService.leave(principal.getUsername(), id);
    }
}
