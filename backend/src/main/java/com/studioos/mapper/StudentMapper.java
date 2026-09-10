package com.studioos.mapper;

import com.studioos.dto.StudentDTO;
import com.studioos.model.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {
    public StudentDTO toDto(Student student) {
        return new StudentDTO(
            student.getId(), student.getTenantId(), student.getBranchId(), student.getFirstName(), student.getLastName(),
            student.getDateOfBirth(), student.getGender(), student.getEmail(), student.getPhone(),
            student.getAddress(), student.getEmergencyContact(), student.getParentName(), student.getParentPhone(),
            student.getDanceStyle(), student.getSkillLevel(), student.getMedicalNotes(), student.getStudentPhotoUrl(),
            student.isActive(), student.getCreatedAt(), student.getUpdatedAt()
        );
    }

    public Student toEntity(StudentDTO dto) {
        Student student = new Student();
        updateEntity(student, dto);
        return student;
    }

    public void updateEntity(Student student, StudentDTO dto) {
        student.setTenantId(valueOrDefault(dto.tenantId(), "default"));
        student.setBranchId(dto.branchId());
        student.setFirstName(dto.firstName().trim());
        student.setLastName(dto.lastName().trim());
        student.setDateOfBirth(dto.dateOfBirth());
        student.setGender(dto.gender());
        student.setEmail(dto.email());
        student.setPhone(dto.phone());
        student.setAddress(dto.address());
        student.setEmergencyContact(dto.emergencyContact());
        student.setParentName(dto.parentName());
        student.setParentPhone(dto.parentPhone());
        student.setDanceStyle(dto.danceStyle());
        student.setSkillLevel(dto.skillLevel());
        student.setMedicalNotes(dto.medicalNotes());
        student.setStudentPhotoUrl(dto.studentPhotoUrl());
        if (dto.active() != null) {
            student.setActive(dto.active());
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
