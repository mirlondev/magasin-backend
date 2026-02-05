package org.odema.posnew.service;

import org.odema.posnew.entity.Store;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.StoreType;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public interface PermissionService {

     void validateStoreManagementPermission(User user, Store store) throws UnauthorizedException;

     void validateEmployeeManagementPermission(User manager, User employee) throws UnauthorizedException ;


}
