package com.neobankengine.repository;

import com.neobankengine.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>
{
    List<Transaction> findByAccountId(Long accountId);
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);
    List<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId);
    List<Transaction> findByAccountIdAndTimestampBetweenOrderByTimestampDesc(Long accountId, LocalDateTime start, LocalDateTime end);
    List<Transaction> findByAccountIdAndTimestampAfterOrderByTimestampDesc(Long accountId, LocalDateTime start);
    List<Transaction> findByAccountIdAndTimestampBeforeOrderByTimestampDesc(Long accountId, LocalDateTime end);
    List<Transaction> findByAccountIdAndTimestampBefore(Long accountId, LocalDateTime timestamp);
    List<Transaction> findByAccountIdInAndTimestampBetween(List<Long> accountIds, LocalDateTime start, LocalDateTime end);
    long countByAccountIdInAndTimestampBetween(List<Long> accountIds, LocalDateTime start, LocalDateTime end);



}
