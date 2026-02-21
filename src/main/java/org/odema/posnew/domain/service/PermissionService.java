package org.odema.posnew.application.service;

import org.odema.posnew.api.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public interface PermissionService {

     void validateStoreManagementPermission(User user, Store store) throws UnauthorizedException;

     void validateEmployeeManagementPermission(User manager, User employee) throws UnauthorizedException ;


}
