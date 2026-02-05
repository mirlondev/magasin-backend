package org.odema.posnew.repository;

import org.odema.posnew.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByName(String name);

    List<Category> findByParentCategoryIsNull();

    List<Category> findByParentCategory_CategoryId(UUID parentId);

    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.name")
    List<Category> findAllActiveCategories();

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Category> searchByName(@Param("name") String name);

    boolean existsByNameAndParentCategoryIsNull(String name);

    boolean existsByNameAndParentCategory_CategoryId(String name, UUID parentId);
}