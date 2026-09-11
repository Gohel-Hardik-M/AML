package com.aml.system.repository;

import com.aml.system.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {
	List<Batch> findAllByOrderByUploadedAtDesc();
	Page<Batch> findAllByOrderByUploadedAtDesc(Pageable pageable);

}
