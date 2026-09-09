package com.kisansarthi.mandi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MandiRepository extends JpaRepository<Mandi, String> {
    List<Mandi> findByDistrictIgnoreCase(String district);
}
