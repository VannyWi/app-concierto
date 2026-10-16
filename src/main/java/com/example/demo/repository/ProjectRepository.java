package com.example.demo.repository;

import com.example.demo.entity.ProjectEntity;
import com.example.demo.dto.response.ProjectExecutiveResponse;
import jakarta.persistence.QueryHint;
import java.util.stream.Stream;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID>, JpaSpecificationExecutor<ProjectEntity> {
    boolean existsByYear(int year);
    boolean existsByYearBetween(int fromYear, int toYear);

    long countByYearAndStatus(int year, com.example.demo.entity.ProjectStatus status);

    long countByYearAndApproved(int year, com.example.demo.entity.ProjectApproval approved);

    List<ProjectEntity> findByYearBetween(int fromYear, int toYear);
    List<ProjectEntity> findByYearBetweenAndCompanyId(int fromYear, int toYear, UUID companyId);

    @Query("select distinct new com.example.demo.dto.response.ProjectExecutiveResponse(p.createdBy.id, p.createdBy.firstName, p.createdBy.lastName, p.createdBy.username) from ProjectEntity p order by p.createdBy.firstName, p.createdBy.lastName")
    List<ProjectExecutiveResponse> findDistinctExecutives();

    @Query("select distinct p.year from ProjectEntity p order by p.year")
    List<Integer> findDistinctYears();

    @Query("select p from ProjectEntity p join fetch p.company where p.year between :fromYear and :toYear order by p.year, p.eventDate, p.projectName")
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "100"))
    Stream<ProjectEntity> streamByYearRange(@Param("fromYear") int fromYear, @Param("toYear") int toYear);
}
