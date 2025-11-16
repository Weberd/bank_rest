package com.example.bankcards.repository;

import com.example.bankcards.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id")
    Optional<Card> findByIdForUpdate(@Param("id") Long id);

    Page<Card> findByUserId(Long userId, Pageable pageable);

    Page<Card> findByStatus(Card.CardStatus status, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.status = :status")
    Page<Card> findByUserIdAndStatus(@Param("userId") Long userId,
                                     @Param("status") Card.CardStatus status,
                                     Pageable pageable);

    boolean existsByCardNumberEncrypted(String cardNumberEncrypted);

    @Query("SELECT c FROM Card c WHERE c.id = :cardId AND c.user.id = :userId")
    Optional<Card> findByIdAndUserId(@Param("cardId") Long cardId, @Param("userId") Long userId);
}