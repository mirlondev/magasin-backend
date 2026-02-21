package org.odema.posnew.application.serviceImpl;


import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.User;
import org.odema.posnew.domain.model.enums.StoreType;
import org.odema.posnew.domain.model.enums.UserRole;
import org.odema.posnew.domain.service.PermissionService;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl  implements PermissionService {

    public void validateStoreManagementPermission(User user, Store store) throws UnauthorizedException {
        if (user.getUserRole() == UserRole.ADMIN) {
            return; // L'admin peut tout faire
        }

        if (store == null || user.getAssignedStore() == null) {
            throw new UnauthorizedException("Permission insuffisante");
        }

        boolean hasPermission = false;

        switch (user.getUserRole()) {
            case DEPOT_MANAGER:
                hasPermission = store.getStoreType().equals(StoreType.WAREHOUSE) &&
                        store.getStoreId().equals(user.getAssignedStore().getStoreId());
                break;
            case STORE_ADMIN:
                hasPermission = store.getStoreType().equals(StoreType.SHOP) &&
                        store.getStoreId().equals(user.getAssignedStore().getStoreId());
                break;
            default:
                hasPermission = false;
        }

        if (!hasPermission) {
            throw new UnauthorizedException(
                    "Vous n'avez pas la permission de gérer ce store");
        }
    }

    public void validateEmployeeManagementPermission(User manager, User employee) throws UnauthorizedException {
        if (manager.getUserRole() == UserRole.ADMIN) {
            return;
        }

        if (employee == null || employee.getAssignedStore() == null ||
                manager.getAssignedStore() == null) {
            throw new UnauthorizedException("Permission insuffisante");
        }

        // Un manager ne peut gérer que les employés de son propre store
        if (!employee.getAssignedStore().getStoreId()
                .equals(manager.getAssignedStore().getStoreId())) {
            throw new UnauthorizedException(
                    "Vous ne pouvez gérer que les employés de votre store");
        }

        // Un manager ne peut pas gérer un autre manager de même niveau
        if ((manager.getUserRole() == UserRole.DEPOT_MANAGER &&
                employee.getUserRole() == UserRole.DEPOT_MANAGER) ||
                (manager.getUserRole() == UserRole.STORE_ADMIN &&
                        employee.getUserRole() == UserRole.STORE_ADMIN)) {
            throw new UnauthorizedException(
                    "Vous ne pouvez pas gérer un autre manager de même niveau");
        }
    }
}
