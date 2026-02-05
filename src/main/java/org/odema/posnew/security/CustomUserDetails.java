package org.odema.posnew.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Custom UserDetails implementation that includes the user's UUID
 * This allows controllers to easily access the user ID from the security context
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    /**
     * Get the user's UUID - this is what you should use in controllers!
     */
    public UUID getUserId() {
        return user.getUserId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return role with ROLE_ prefix for Spring Security
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getActive();
    }

    /**
     * Get the full user object if needed
     */
    public User getUser() {
        return user;
    }

    /**
     * Get user's email
     */
    public String getEmail() {
        return user.getEmail();
    }
}