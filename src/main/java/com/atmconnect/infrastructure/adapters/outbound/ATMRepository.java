package com.atmconnect.infrastructure.adapters.outbound;

import com.atmconnect.domain.entities.ATM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ATMRepository extends JpaRepository<ATM, String> {
    
    Optional<ATM> findByAtmCode(String atmCode);
    
    Optional<ATM> findByBluetoothMacAddress(String bluetoothMacAddress);
    
    List<ATM> findByActiveTrue();
    
    List<ATM> findByActiveTrueAndOnlineTrue();
    
    @Query("SELECT a FROM ATM a WHERE a.active = true AND a.online = true AND a.cashAvailable = true")
    List<ATM> findAvailableATMs();
    
    @Query("SELECT a FROM ATM a WHERE " +
           "(:latitude - a.latitude) * (:latitude - a.latitude) + " +
           "(:longitude - a.longitude) * (:longitude - a.longitude) <= :radiusSquared " +
           "AND a.active = true")
    List<ATM> findATMsNearLocation(@Param("latitude") Double latitude, 
                                  @Param("longitude") Double longitude, 
                                  @Param("radiusSquared") Double radiusSquared);
    
    @Query("SELECT a FROM ATM a WHERE a.lastHeartbeat < :threshold")
    List<ATM> findATMsWithOldHeartbeat(@Param("threshold") LocalDateTime threshold);
    
    boolean existsByAtmCode(String atmCode);
    
    boolean existsByBluetoothMacAddress(String bluetoothMacAddress);
}