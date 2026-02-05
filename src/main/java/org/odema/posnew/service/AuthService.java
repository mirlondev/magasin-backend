package org.odema.posnew.service;

import org.odema.posnew.dto.request.LoginRequest;
import org.odema.posnew.dto.request.RegisterRequest;
import org.odema.posnew.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    LoginResponse register(RegisterRequest registerRequest);
}
