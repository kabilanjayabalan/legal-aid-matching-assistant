package com.legalaid.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.legalaid.backend.model.Profile;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Integer> {
    Optional<Profile> findByUser(User user);
    Optional<Profile> findByUserRole(Role role);

    @Modifying
    @Transactional
    @Query(value = """
INSERT INTO profiles (role, organization_name, city, state, is_verified)
VALUES ('NGO', :orgName, :city, :state, true)
ON CONFLICT (organization_name)
DO UPDATE SET
city = EXCLUDED.city,
state = EXCLUDED.state
""", nativeQuery = true)
    void upsertNgoProfile(
            @Param("orgName") String orgName,
            @Param("city") String city,
            @Param("state") String state
    );
    @Query("""
        SELECT np.latitude, np.longitude , COUNT(np)
        FROM Profile np
        WHERE np.latitude IS NOT NULL AND np.longitude IS NOT NULL
        GROUP BY np.latitude, np.longitude
        """)
        List<Object[]> countCitizensByGeo();

}
