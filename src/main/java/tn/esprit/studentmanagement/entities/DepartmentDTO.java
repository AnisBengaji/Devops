package tn.esprit.studentmanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentDTO {
    private Long idDepartment;   // used by update, null on create
    private String name;
    private String location;
    private String phone;
    private String head;
}