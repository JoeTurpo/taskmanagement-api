package com.org.repository;

import com.org.model.Task;
import com.org.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByTitle(String title);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.developer.id= :developerId AND t.status IN :statuses")
    int countActiveTasksByDeveloperId(Long developerId, List<TaskStatus> statuses);

    Page<Task> findAll(Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.startDate >= :start AND t.endDate <= :end")
    Page<Task> findTasksByDateRange(LocalDate start, LocalDate end, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.developer.id= :developerId AND t.status IN :statuses")
    List<Task> findTasksByDeveloperAndStatusIn(Long developerId, List<TaskStatus> statuses);

}
