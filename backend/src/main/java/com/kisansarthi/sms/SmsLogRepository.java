package com.kisansarthi.sms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, UUID> {
    List<SmsLog> findTop50ByOrderByCreatedAtDesc();
    List<SmsLog> findByRecipientPhoneOrderByCreatedAtDesc(String phone);
}
