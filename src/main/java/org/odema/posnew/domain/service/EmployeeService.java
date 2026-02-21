package org.odema.posnew.application.service;

import org.odema.posnew.application.dto.request.EmployeeRequest;
import org.odema.posnew.application.dto.response.EmployeeResponse;
import org.odema.posnew.application.dto.request.EmployeeUpdateRequest;
import org.odema.posnew.domain.enums_old.UserRole;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {

    // Créer un employé pour un store (boutique ou dépôt)
    EmployeeResponse createStoreEmployee(EmployeeRequest request, UUID storeId);

    // Créer un employé de dépôt (warehouse)
    EmployeeResponse createWarehouseEmployee(EmployeeRequest request, UUID storeId);

    // Obtenir tous les employés (avec filtres optionnels)
    List<EmployeeResponse> findAllEmployees(UUID storeId, UserRole role);

    // Obtenir un employé par ID
    EmployeeResponse getEmployeeById(UUID employeeId);

    // Mettre à jour un employé
    EmployeeResponse updateEmployee(UUID employeeId, EmployeeUpdateRequest request);

    // Désactiver un employé (soft delete)
    void deactivateEmployee(UUID employeeId);

    // Réactiver un employé
    void activateEmployee(UUID employeeId);

    // Obtenir les employés par rôle dans un store spécifique
    List<EmployeeResponse> getEmployeesByRoleInStore(UUID storeId, UserRole role);

    // Obtenir les managers d'un store
    List<EmployeeResponse> getStoreManagers(UUID storeId);

    // Obtenir les caissiers d'un store
    List<EmployeeResponse> getStoreCashiers(UUID storeId);

    // Transférer un employé vers un autre store
    EmployeeResponse transferEmployee(UUID employeeId, UUID newStoreId);

    // Changer le rôle d'un employé
    EmployeeResponse changeEmployeeRole(UUID employeeId, UserRole newRole);
}
