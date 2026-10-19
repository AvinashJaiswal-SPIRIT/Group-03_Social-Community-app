package com.smartCommunity.SmartCommunity.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser,Long> {

    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

    // Shortcut used by the community/dashboard services:
    // the JWT only carries the email, so we load the full user from it.
    default AppUser getByEmailOrThrow(String email) {
        return findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
