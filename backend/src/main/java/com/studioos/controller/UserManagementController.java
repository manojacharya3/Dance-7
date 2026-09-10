package com.studioos.controller;

import com.studioos.dto.UserManagementDTO;
import com.studioos.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {
    private final UserService service;
    public UserManagementController(UserService service) { this.service = service; }
    @GetMapping public List<UserManagementDTO> list(Authentication authentication) { requireOwnerOrDeveloper(authentication); return service.listManagedUsers("default"); }
    @PostMapping public UserManagementDTO create(Authentication authentication, @Valid @RequestBody UserManagementDTO dto) { requireOwner(authentication); return service.createManagedUser(dto); }
    @PutMapping("/{id}") public UserManagementDTO update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UserManagementDTO dto) { requireOwner(authentication); return service.updateManagedUser(id, dto); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> disable(Authentication authentication, @PathVariable Long id, @RequestParam(defaultValue = "false") boolean permanent) { requireOwner(authentication); if (permanent) service.removeManagedUser(id, authentication == null ? null : authentication.getName()); else service.disableManagedUser(id); return ResponseEntity.noContent().build(); }
    private void requireOwner(Authentication authentication) { if (authentication == null || authentication.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_OWNER"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner access required"); }
    private void requireOwnerOrDeveloper(Authentication authentication) { if (authentication == null || authentication.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") || authority.getAuthority().equals("ROLE_OWNER") || authority.getAuthority().equals("ROLE_DEVELOPER"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner access required"); }
}
