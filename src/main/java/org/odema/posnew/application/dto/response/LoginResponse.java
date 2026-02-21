package org.odema.posnew.application.dto.response;



import org.odema.posnew.domain.enums_old.UserRole;

import java.util.UUID;


public record LoginResponse (

     String token,
     UUID userId,
     String username,
     String email, UserRole  userRole) {}