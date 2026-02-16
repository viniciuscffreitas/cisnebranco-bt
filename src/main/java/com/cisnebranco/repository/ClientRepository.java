package com.cisnebranco.repository;

import com.cisnebranco.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {
    List<Client> findByNameContainingIgnoreCase(String name);
    Page<Client> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
