package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.request.LoginRequest;
import org.odema.posnew.application.dto.request.RegisterRequest;
import org.odema.posnew.application.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    LoginResponse register(RegisterRequest registerRequest);
}
