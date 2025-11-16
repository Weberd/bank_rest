package com.example.bankcards.repository;

import com.example.bankcards.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardEventRepository extends JpaRepository<CardEvent, Long> {
}