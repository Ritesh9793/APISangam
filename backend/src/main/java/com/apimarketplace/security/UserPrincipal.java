package com.apimarketplace.security;

import com.apimarketplace.entity.UserAccount;
import com.apimarketplace.entity.enums.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean mfaEnabled;
    private final boolean enabled;

    public UserPrincipal(UUID id, String email, String passwordHash, UserRole role, boolean mfaEnabled, boolean enabled) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.mfaEnabled = mfaEnabled;
        this.enabled = enabled;
    }

    public static UserPrincipal from(UserAccount user) {
        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getRole(),
            user.isMfaEnabled(),
            user.isEnabled()
        );
    }

    public UUID getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isMfaEnabledFlag() {
        return mfaEnabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
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
        return enabled;
    }
}
