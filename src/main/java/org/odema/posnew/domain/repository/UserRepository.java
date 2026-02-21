package org.odema.posnew.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByUserRole(UserRole role);

    List<User> findByUserRoleNot(UserRole role);

    List<User> findByAssignedStore_StoreId(UUID storeId);

    List<User> findByAssignedStore_StoreIdAndUserRole(UUID storeId, UserRole role);

    Page<User> findByActiveTrue(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.active = true AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> searchActiveUsers(@Param("search") String search);
}
