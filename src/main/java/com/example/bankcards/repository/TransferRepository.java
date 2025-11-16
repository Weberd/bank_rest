package com.example.bankcards.repository;

import com.example.bankcards.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Page<Transfer> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE (t.fromCard.id = :cardId OR t.toCard.id = :cardId) AND t.userId = :userId ORDER BY t.createdAt DESC")
    Page<Transfer> findByCardIdAndUserId(@Param("cardId") Long cardId,
                                         @Param("userId") Long userId,
                                         Pageable pageable);
}
