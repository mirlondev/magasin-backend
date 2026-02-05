package org.odema.posnew.repository;

import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    List<User> findByAssignedStore_StoreId(UUID storeId);

    List<User> findByAssignedStore_StoreIdAndUserRole(UUID storeId, UserRole userRole);

    List<User> findByUserRole(UserRole userRole);

    List<User> findByUserRoleNot(UserRole userRole);

    Optional<User> findByPhone(String phone);

    //    @Query("SELECT u FROM User u WHERE u.assignedStore.storeId = :storeId " +
//            "AND u.userRole IN :roles AND u.active = true")
//    List<User> findByStoreAndRolesIn(
//            @Param("storeId") String storeId,
//            @Param("roles") List<UserRole> roles);

//    @Query("SELECT COUNT(u) FROM User u WHERE u.assignedStore.storeId = :storeId " +
//            "AND u.active = true")
//    Long countActiveEmployeesByStore(@Param("storeId") String storeId);
}
