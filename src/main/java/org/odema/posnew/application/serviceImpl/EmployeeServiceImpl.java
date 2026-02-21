package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.api.exception.BusinessException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.EmployeeResponse;
import org.odema.posnew.application.dto.request.EmployeeRequest;
import org.odema.posnew.application.dto.request.EmployeeUpdateRequest;
import org.odema.posnew.application.mapper.EmployeeMapper;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.User;
import org.odema.posnew.domain.model.enums.StoreType;
import org.odema.posnew.domain.model.enums.UserRole;
import org.odema.posnew.domain.repository.StoreRepository;
import org.odema.posnew.domain.repository.UserRepository;
import org.odema.posnew.domain.service.EmployeeService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public EmployeeResponse createStoreEmployee(EmployeeRequest request, UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        if (!store.getStoreType().equals(StoreType.SHOP)) {
            throw new BusinessException("Ce store n'est pas une boutique. Utilisez createWarehouseEmployee pour les dépôts.");
        }

        if (!isValidShopRole(request.role())) {
            throw new BusinessException(
                    "Rôle invalide pour une boutique. Rôles autorisés: SHOP_MANAGER, CASHIER, EMPLOYEE");
        }

        return createEmployee(request, store);
    }

    @Override
    @Transactional
    public EmployeeResponse createWarehouseEmployee(EmployeeRequest request, UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        if (!store.getStoreType().equals(StoreType.WAREHOUSE)) {
            throw new BusinessException("Ce store n'est pas un dépôt. Utilisez createStoreEmployee pour les boutiques.");
        }

        if (!isValidDepotRole(request.role())) {
            throw new BusinessException(
                    "Rôle invalide pour un dépôt. Rôles autorisés: DEPOT_MANAGER, EMPLOYEE");
        }

        return createEmployee(request, store);
    }

    private EmployeeResponse createEmployee(EmployeeRequest request, Store store) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ce nom d'utilisateur est déjà utilisé");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            userRepository.findByPhone(request.phone()).ifPresent(u -> {
                throw new BusinessException("Ce numéro de téléphone est déjà utilisé");
            });
        }

        User user = employeeMapper.toEntity(request, store);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAssignedStore(store);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        return employeeMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAllEmployees(UUID storeId, UserRole role) {
        List<User> users;

        if (storeId != null && role != null) {
            users = userRepository.findByAssignedStore_StoreIdAndUserRole(storeId, role);
        } else if (storeId != null) {
            users = userRepository.findByAssignedStore_StoreId(storeId);
        } else if (role != null) {
            users = userRepository.findByUserRole(role);
        } else {
            users = userRepository.findByUserRoleNot(UserRole.ADMIN);
        }

        return users.stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(UUID employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé non trouvé"));

        return employeeMapper.toResponse(user);
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(UUID employeeId, EmployeeUpdateRequest request) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé non trouvé"));

        if (request.username() != null &&
                !request.username().equals(user.getUsername()) &&
                userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ce nom d'utilisateur est déjà utilisé");
        }

        if (request.email() != null &&
                !request.email().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        if (request.phone() != null &&
                !request.phone().equals(user.getPhone())) {
            userRepository.findByPhone(request.phone()).ifPresent(u -> {
                if (!u.getUserId().equals(employeeId)) {
                    throw new BusinessException("Ce numéro de téléphone est déjà utilisé");
                }
            });
        }

        Store store = null;
        if (request.storeId() != null) {
            store = storeRepository.findById(request.storeId())
                    .orElseThrow(() -> new NotFoundException("Store non trouvé"));

            validateRoleStoreCompatibility(request.role(), store);
        }

        employeeMapper.updateEntityFromRequest(user, request, store);

        User updatedUser = userRepository.save(user);
        return employeeMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deactivateEmployee(UUID employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé non trouvé"));

        if (!user.getActive()) {
            throw new BusinessException("L'employé est déjà désactivé");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateEmployee(UUID employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé non trouvé"));

        if (user.getActive()) {
            throw new BusinessException("L'employé est déjà activé");
        }

        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByRoleInStore(UUID storeId, UserRole role) {
        List<User> users = userRepository.findByAssignedStore_StoreIdAndUserRole(storeId, role);

        return users.stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getStoreManagers(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        UserRole managerRole = store.getStoreType().equals(StoreType.SHOP)
                ? UserRole.STORE_ADMIN
                : UserRole.DEPOT_MANAGER;

        return getEmployeesByRoleInStore(storeId, managerRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getStoreCashiers(UUID storeId) {
        return getEmployeesByRoleInStore(storeId, UserRole.CASHIER);
    }

    @Override
    @Transactional
    public EmployeeResponse transferEmployee(UUID employeeId, UUID newStoreId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé non trouvé"));

        Store newStore = storeRepository.findById(newStoreId)
                .orElseThrow(() -> new NotFoundException("Nouveau store non trouvé"));

        validateRoleStoreCompatibility(user.getUserRole(), newStore);

        user.setAssignedStore(newStore);

        User updatedUser = userRepository.save(user);
        return employeeMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public EmployeeResponse changeEmployeeRole(UUID employeeId, UserRole newRole) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé non trouvé"));

        if (user.getAssignedStore() == null) {
            throw new BusinessException("L'employé n'a pas de store assigné");
        }

        validateRoleStoreCompatibility(newRole, user.getAssignedStore());

        user.setUserRole(newRole);

        User updatedUser = userRepository.save(user);
        return employeeMapper.toResponse(updatedUser);
    }

    private boolean isValidShopRole(UserRole role) {
        return role == UserRole.STORE_ADMIN ||
                role == UserRole.CASHIER ||
                role == UserRole.EMPLOYEE;
    }

    private boolean isValidDepotRole(UserRole role) {
        return role == UserRole.DEPOT_MANAGER ||
                role == UserRole.EMPLOYEE;
    }

    private void validateRoleStoreCompatibility(UserRole role, Store store) {
        if (role == null || store == null) return;

        if (store.getStoreType().equals(StoreType.SHOP) && !isValidShopRole(role)) {
            throw new BusinessException(
                    "Le rôle " + role + " n'est pas compatible avec une boutique");
        }

        if (store.getStoreType().equals(StoreType.WAREHOUSE) && !isValidDepotRole(role)) {
            throw new BusinessException(
                    "Le rôle " + role + " n'est pas compatible avec un dépôt");
        }
    }
}