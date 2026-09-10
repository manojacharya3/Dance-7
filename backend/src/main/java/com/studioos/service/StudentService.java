package com.studioos.service;

import com.studioos.dto.StudentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentService {
    StudentDTO createStudent(StudentDTO studentDTO);
    StudentDTO updateStudent(Long id, StudentDTO studentDTO);
    StudentDTO getStudentById(Long id, String tenantId);
    Page<StudentDTO> listStudents(String tenantId, String search, Pageable pageable);
    void softDeleteStudent(Long id, String tenantId);
}
