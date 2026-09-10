package com.studioos.service;

import com.studioos.dto.StudentDTO;
import com.studioos.mapper.StudentMapper;
import com.studioos.model.Student;
import com.studioos.repository.StudentRepository;
import com.studioos.repository.BranchRepository;
import com.studioos.config.ScopeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final BranchRepository branchRepository;
    private final ScopeService scope;

    public StudentServiceImpl(StudentRepository studentRepository, StudentMapper studentMapper, BranchRepository branchRepository, ScopeService scope) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
        this.branchRepository = branchRepository;
        this.scope = scope;
    }

    @Override
    public StudentDTO createStudent(StudentDTO studentDTO) {
        Long branchId = scope.resolveBranchForWrite(studentDTO.branchId());
        validateBranch(branchId, tenantIdFrom(studentDTO.tenantId()));
        Student saved = studentMapper.toEntity(withBranch(studentDTO, branchId));
        Student student = studentRepository.save(saved);
        return studentMapper.toDto(student);
    }

    @Override
    public StudentDTO updateStudent(Long id, StudentDTO studentDTO) {
        String tenantId = tenantIdFrom(studentDTO.tenantId());
        Long branchId = scope.resolveBranchForWrite(studentDTO.branchId());
        validateBranch(branchId, tenantId);
        Student student = findActiveStudent(id, tenantId);
        scope.enforceBranch(student.getBranchId());
        studentMapper.updateEntity(student, withBranch(studentDTO, branchId));
        return studentMapper.toDto(studentRepository.save(student));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long id, String tenantId) {
        Student student = findActiveStudent(id, tenantIdFrom(tenantId));
        scope.enforceBranch(student.getBranchId());
        return studentMapper.toDto(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDTO> listStudents(String tenantId, String search, Pageable pageable) {
        String normalizedTenant = tenantIdFrom(tenantId);
        Long scopedBranch = scope.effectiveBranchId();
        Page<Student> students;
        if (scopedBranch != null) {
            students = search == null || search.isBlank()
                ? studentRepository.findByTenantIdAndBranchIdAndActiveTrue(normalizedTenant, scopedBranch, pageable)
                : studentRepository.searchActiveStudentsByBranch(normalizedTenant, scopedBranch, search.trim(), pageable);
        } else {
            students = search == null || search.isBlank()
                ? studentRepository.findByTenantIdAndActiveTrue(normalizedTenant, pageable)
                : studentRepository.searchActiveStudents(normalizedTenant, search.trim(), pageable);
        }
        return students.map(studentMapper::toDto);
    }

    @Override
    public void softDeleteStudent(Long id, String tenantId) {
        Student student = findActiveStudent(id, tenantIdFrom(tenantId));
        scope.enforceBranch(student.getBranchId());
        student.setActive(false);
        studentRepository.save(student);
    }

    private Student findActiveStudent(Long id, String tenantId) {
        return studentRepository.findByIdAndTenantIdAndActiveTrue(id, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + id));
    }

    private String tenantIdFrom(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim();
    }

    private StudentDTO withBranch(StudentDTO dto, Long branchId) {
        return new StudentDTO(dto.id(), dto.tenantId(), branchId, dto.firstName(), dto.lastName(),
            dto.dateOfBirth(), dto.gender(), dto.email(), dto.phone(), dto.address(),
            dto.emergencyContact(), dto.parentName(), dto.parentPhone(), dto.danceStyle(),
            dto.skillLevel(), dto.medicalNotes(), dto.studentPhotoUrl(), dto.active(),
            dto.createdAt(), dto.updatedAt());
    }
    private void validateBranch(Long branchId, String tenantId) { if (branchRepository.findByIdAndTenantIdAndActiveTrue(branchId, tenantId).isEmpty()) throw new EntityNotFoundException("Branch not found: " + branchId); }
}
