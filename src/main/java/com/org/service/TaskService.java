package com.org.service;

import com.org.dto.request.TaskRequest;
import com.org.dto.response.TaskResponse;
import com.org.exception.BusinessRuleException;
import com.org.exception.DuplicateResourceException;
import com.org.exception.ResourceNotFoundException;
import com.org.mapper.TaskMapper;
import com.org.model.Developer;
import com.org.model.Task;
import com.org.model.enums.TaskStatus;
import com.org.repository.DeveloperRepository;
import com.org.repository.TaskRepository;
import jakarta.persistence.Temporal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final DeveloperRepository developerRepository;
    private final TaskMapper taskMapper;

    private static final int MAX_ACTIVE_TASKS = 5;

    @Transactional
    public TaskResponse create(TaskRequest request){
        if(taskRepository.existsByTitle(request.title())){
            throw new DuplicateResourceException("Ya existe una tarea con ese titulo");
        }

        Developer developer = developerRepository.findById(request.developerId())
                .orElseThrow(() -> new ResourceNotFoundException("Developer no encontrado"));

        int activeCount = taskRepository.countActiveTasksByDeveloperId(
                developer.getId(), List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)
        );

        if(activeCount>=MAX_ACTIVE_TASKS){
            throw new BusinessRuleException("El developer no puede tener mas de "+MAX_ACTIVE_TASKS+" tareas activas");
        }

        Task task = taskMapper.toEntity(request, developer);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(Pageable pageable){
        return taskRepository.findAll(pageable).map(taskMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));

        if(!task.getTitle().equals(request.title()) && taskRepository.existsByTitle(request.title())){
            throw new DuplicateResourceException("Ya existe una tarea con ese titulo");
        }

        if(task.getStatus() != TaskStatus.PENDING){
            throw new BusinessRuleException("Solo se puede modificar una tarea PENDING");
        }

        Developer newDeveloper = developerRepository.findById(request.developerId())
                .orElseThrow(() -> new ResourceNotFoundException("Developer no encontrado"));

        if(!task.getDeveloper().getId().equals(newDeveloper.getId())){
            int activeCount = taskRepository.countActiveTasksByDeveloperId(
                    newDeveloper.getId(), List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)
            );
            if(activeCount>=MAX_ACTIVE_TASKS){
                throw new BusinessRuleException("El nuevo developer tiene tareas activas al limite");
            }
            task.setDeveloper(newDeveloper);
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long id, TaskStatus newStatus){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
        TaskStatus current = task.getStatus();

        if(newStatus == TaskStatus.COMPLETED && current != TaskStatus.IN_PROGRESS){
            throw new BusinessRuleException("Solo se puede completar una tarea que estuvo IN_PROGRESS");
        }

        if(newStatus == TaskStatus.IN_PROGRESS && current != TaskStatus.PENDING){
            throw new BusinessRuleException("Solo se puede iniciar una tarea que esta en PENDING");
        }

        task.setStatus(newStatus);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findByDateRange(LocalDate start, LocalDate end, Pageable pageable){
        if(start.isAfter(end)){
            throw new BusinessRuleException("La fecha de inicio debe ser anterior a la de fin");
        }
        return taskRepository.findTasksByDateRange(start, end, pageable)
                .map(taskMapper::toResponse);
    }


    @Transactional(readOnly = true)
    public List<TaskResponse> findActiveTasksByDeveloper(Long developerId){
        List<Task> tasks = taskRepository.findTasksByDeveloperAndStatusIn(
                developerId, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)
        );
        return tasks.stream().map(taskMapper::toResponse).toList();
    }
}
