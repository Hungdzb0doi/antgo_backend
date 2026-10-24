package com.flashjobweb.repository;

import com.flashjobweb.entity.WorkerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfileEntity, UUID> {

    List<WorkerProfileEntity> findByIsAvailableTrue();

    @Query(value = "SELECT w FROM WorkerProfileEntity w join users u in w.user_id=u.user_id" +
            "WHERE u.current_mode='WORKER' and w.is_available=true and ST_DWithin(w.current_location::geography,ST_SetSRID(ST_MakePoint(:longitude,:latitude),4326)::geography,:radiusinkm*1000)))",nativeQuery = true)
    List<WorkerProfileEntity> findAllWorkerNearbyJob(@Param("latitude" ) double latitude,@Param("longitude" ) double longitude,@Param("radiusinkm" ) double radiusInKm);


}
