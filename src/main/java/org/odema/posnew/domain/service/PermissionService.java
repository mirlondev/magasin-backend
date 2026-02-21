package org.odema.posnew.domain.service;

import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public interface PermissionService {

     void validateStoreManagementPermission(User user, Store store) throws UnauthorizedException;

     void validateEmployeeManagementPermission(User manager, User employee) throws UnauthorizedException ;


}
