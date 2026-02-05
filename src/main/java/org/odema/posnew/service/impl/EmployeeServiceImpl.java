package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.EmployeeRequest;
import org.odema.posnew.dto.response.EmployeeResponse;
import org.odema.posnew.dto.request.EmployeeUpdateRequest;
import org.odema.posnew.entity.Store;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.StoreType;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.EmployeeMapper;
import org.odema.posnew.repository.StoreRepository;
import org.odema.posnew.repository.UserRepository;
import org.odema.posnew.service.EmployeeService;
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
        // Vérifier que le store existe
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        // Vérifier que le store est une boutique
        if (!store.getStoreType().equals(StoreType.SHOP)) {
            throw new BadRequestException("Ce store n'est pas une boutique. Utilisez createWarehouseEmployee pour les dépôts.");
        }

        // Vérifier que le rôle est compatible avec une boutique
        if (!isValidShopRole(request.role())) {
            throw new BadRequestException(
                    "Rôle invalide pour une boutique. Rôles autorisés: SHOP_MANAGER, CASHIER, EMPLOYEE");
        }

        return createEmployee(request, store);
    }

    @Override
    @Transactional
    public EmployeeResponse createWarehouseEmployee(EmployeeRequest request, UUID storeId) {
        // Vérifier que le store existe
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        // Vérifier que le store est un dépôt
        if (!store.getStoreType().equals(StoreType.WAREHOUSE)) {
            throw new BadRequestException("Ce store n'est pas un dépôt. Utilisez createStoreEmployee pour les boutiques.");
        }

        // Vérifier que le rôle est compatible avec un dépôt
        if (!isValidDepotRole(request.role())) {
            throw new BadRequestException(
                    "Rôle invalide pour un dépôt. Rôles autorisés: DEPOT_MANAGER, EMPLOYEE");
        }

        return createEmployee(request, store);
    }

    private EmployeeResponse createEmployee(EmployeeRequest request, Store store) {
        // Vérifier l'unicité du username
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }

        // Vérifier l'unicité de l'email
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        // Vérifier l'unicité du téléphone si fourni
        if (request.phone() != null && !request.phone().isBlank()) {
            userRepository.findByPhone(request.phone()).ifPresent(u -> {
                throw new BadRequestException("Ce numéro de téléphone est déjà utilisé");
            });
        }

        // Créer l'utilisateur
        User user = employeeMapper.toEntity(request, store);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAssignedStore(store);

        User savedUser = userRepository.save(user);
        return employeeMapper.toResponse(savedUser);
    }

    @Override
    public List<EmployeeResponse> findAllEmployees(UUID storeId, UserRole role) {
        List<User> users;

        if (storeId != null && role != null) {
            // Filtrer par store et rôle
            users = userRepository.findByAssignedStore_StoreIdAndUserRole(
                    storeId, role);
        } else if (storeId != null) {
            // Filtrer seulement par store
            users = userRepository.findByAssignedStore_StoreId(storeId);
        } else if (role != null) {
            // Filtrer seulement par rôle
            users = userRepository.findByUserRole(role);
        } else {
            // Tous les employés (exclure les admins)
            users = userRepository.findByUserRoleNot(UserRole.ADMIN);
        }

        return users.stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
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

        // Vérifier l'unicité du username si modifié
        if (request.username() != null &&
                !request.username().equals(user.getUsername()) &&
                userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }

        // Vérifier l'unicité de l'email si modifié
        if (request.email() != null &&
                !request.email().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        // Vérifier l'unicité du téléphone si modifié
        if (request.phone() != null &&
                !request.phone().equals(user.getPhone())) {
            userRepository.findByPhone(request.phone()).ifPresent(u -> {
                if (!u.getUserId().equals(employeeId)) {
                    throw new BadRequestException("Ce numéro de téléphone est déjà utilisé");
                }
            });
        }

        // Récupérer le store si spécifié
        Store store = null;
        if (request.storeId() != null) {
            store = storeRepository.findById(request.storeId())
                    .orElseThrow(() -> new NotFoundException("Store non trouvé"));

            // Vérifier la compatibilité rôle/store
            validateRoleStoreCompatibility(request.role(), store);
        }

        // Mettre à jour l'entité
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
            throw new BadRequestException("L'employé est déjà désactivé");
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
            throw new BadRequestException("L'employé est déjà activé");
        }

        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public List<EmployeeResponse> getEmployeesByRoleInStore(UUID storeId, UserRole role) {
        List<User> users = userRepository.findByAssignedStore_StoreIdAndUserRole(
                storeId, role);

        return users.stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    public List<EmployeeResponse> getStoreManagers(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        UserRole managerRole = store.getStoreType().equals(StoreType.SHOP)
                ? UserRole.STORE_ADMIN
                : UserRole.DEPOT_MANAGER;

        return getEmployeesByRoleInStore(storeId, managerRole);
    }

    @Override
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

        // Vérifier la compatibilité rôle/store
        validateRoleStoreCompatibility(user.getUserRole(), newStore);

        // Mettre à jour le store
        user.setAssignedStore(newStore);

        User updatedUser = userRepository.save(user);
        return employeeMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public EmployeeResponse changeEmployeeRole(UUID employeeId, UserRole newRole) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employé non trouvé"));

        // Vérifier que l'employé a un store assigné
        if (user.getAssignedStore() == null) {
            throw new BadRequestException("L'employé n'a pas de store assigné");
        }

        // Vérifier la compatibilité rôle/store
        validateRoleStoreCompatibility(newRole, user.getAssignedStore());

        // Mettre à jour le rôle
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
            throw new BadRequestException(
                    "Le rôle " + role + " n'est pas compatible avec une boutique");
        }

        if (store.getStoreType().equals(StoreType.WAREHOUSE) && !isValidDepotRole(role)) {
            throw new BadRequestException(
                    "Le rôle " + role + " n'est pas compatible avec un dépôt");
        }
    }
}
