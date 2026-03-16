package com.legalaid.backend.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;

public interface UserRepository extends JpaRepository<User, Integer>
{
    Optional <User> findById(Integer id);
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    List<User> findByRoleIn(List<Role> roles);

    long countByRole(Role role);

        
    @Query("""
    SELECT u FROM User u
    WHERE u.role <> com.legalaid.backend.model.Role.ADMIN
      AND (:search = '' OR LOWER(u.fullName) LIKE %:search% OR LOWER(u.email) LIKE %:search%)
      AND (:role IS NULL OR u.role = :role)
      AND (:status IS NULL OR u.status = :status)
      AND (
        u.role NOT IN (com.legalaid.backend.model.Role.LAWYER, com.legalaid.backend.model.Role.NGO)
        OR u.approved = TRUE
      )
    """)
    Page<User> findUsers(
        @Param("search") String search,
        @Param("role") Role role,
        @Param("status") UserStatus status,
        Pageable pageable
    );

    @Query("""
    SELECT u FROM User u
    WHERE (u.role = 'LAWYER' OR u.role = 'NGO')
      AND (:role IS NULL OR u.role = :role)
      AND (
            (:pendingOnly = true AND u.approved IS NULL)
            OR
            (:pendingOnly = false AND (:approved IS NULL OR u.approved = :approved))
          )
""")
Page<User> findLawyersAndNgos(
        @Param("role") Role role,
        @Param("approved") Boolean approved,
        @Param("pendingOnly") boolean pendingOnly,
        Pageable pageable
);



    @Query("""
SELECT date_trunc('day', u.createdAt), COUNT(u)
FROM User u
GROUP BY date_trunc('day', u.createdAt)
ORDER BY date_trunc('day', u.createdAt)
""")
List<Object[]> countUsersDaily();

@Query("""
SELECT date_trunc('month', u.createdAt), COUNT(u)
FROM User u
GROUP BY date_trunc('month', u.createdAt)
ORDER BY date_trunc('month', u.createdAt)
""")
List<Object[]> countUsersMonthly();

@Query("""
SELECT date_trunc('year', u.createdAt), COUNT(u)
FROM User u
GROUP BY date_trunc('year', u.createdAt)
ORDER BY date_trunc('year', u.createdAt)
""")
List<Object[]> countUsersYearly();


}
