package tn.esprit.studentmanagement.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.studentmanagement.dto.DepartmentDTO;
import tn.esprit.studentmanagement.entities.Department;
import tn.esprit.studentmanagement.services.IDepartmentService;

import java.util.List;

@RestController
@RequestMapping("/Depatment")
@CrossOrigin(origins = "http://localhost:4200")
@AllArgsConstructor
public class DepartmentController {
    private IDepartmentService departmentService;

    @GetMapping("/getAllDepartment")
    public List<Department> getAllDepartment() { return departmentService.getAllDepartments(); }

    @GetMapping("/getDepartment/{id}")
    public Department getDepartment(@PathVariable Long id) { return departmentService.getDepartmentById(id); }

    @PostMapping("/createDepartment")
    public Department createDepartment(@RequestBody DepartmentDTO dto) {
        return departmentService.saveDepartment(toEntity(dto));
    }

    @PutMapping("/updateDepartment")
    public Department updateDepartment(@RequestBody DepartmentDTO dto) {
        return departmentService.saveDepartment(toEntity(dto));
    }

    @DeleteMapping("/deleteDepartment/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }

    // Copie uniquement les champs autorisés du DTO vers l'entité
    private Department toEntity(DepartmentDTO dto) {
        Department department = new Department();
        department.setIdDepartment(dto.getIdDepartment());
        department.setName(dto.getName());
        department.setLocation(dto.getLocation());
        department.setPhone(dto.getPhone());
        department.setHead(dto.getHead());
        return department;
    }
}