package org.odema.posnew.dto.response;



import org.odema.posnew.entity.enums.UserRole;

import java.util.Set;
import java.util.UUID;


public record LoginResponse (

     String token,
     UUID userId,
     String username,
     String email, UserRole  userRole) {}