package com.fullmall.staff.api;

import com.fullmall.staff.domain.StaffUser;
import com.fullmall.staff.infra.StaffAuthRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/v1/staff")
public class StaffAdminController {

    private final StaffAuthRepository repository;

    public StaffAdminController(StaffAuthRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<StaffUser> list() {
        return repository.findAll();
    }
}