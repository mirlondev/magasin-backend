package org.odema.posnew.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByName(String name);

    boolean existsByNameAndParentCategoryIsNull(String name);

    boolean existsByNameAndParentCategory_CategoryId(String name, UUID parentId);

    List<Category> findByParentCategoryIsNull();

    List<Category> findByParentCategory_CategoryId(UUID parentId);

    @Query("SELECT c FROM Category c WHERE c.isActive = true")
    List<Category> findAllActiveCategories();

    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword% AND c.isActive = true")
    List<Category> searchByName(@Param("keyword") String keyword);

    Page<Category> findByIsActiveTrue(Pageable pageable);
}
