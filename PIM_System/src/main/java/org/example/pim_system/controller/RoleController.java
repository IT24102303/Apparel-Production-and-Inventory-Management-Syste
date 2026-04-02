package org.example.pim_system.controller;

import org.example.pim_system.model.Role;
import org.example.pim_system.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<Map<String, Object>> body = roles.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            String name = asString(payload.get("name"));
            String description = asString(payload.get("description"));
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) payload.get("permissions");

            if (name == null || name.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Role name is required!");
                return ResponseEntity.badRequest().body(response);
            }

            if (roleRepository.existsByName(name.trim())) {
                response.put("success", false);
                response.put("message", "A role with this name already exists!");
                return ResponseEntity.badRequest().body(response);
            }

            String permissions = permissionsList != null
                    ? String.join(",", permissionsList)
                    : "";

            Role role = new Role(name.trim(), description, permissions);
            Role saved = roleRepository.save(role);

            response.put("success", true);
            response.put("message", "Role created successfully!");
            response.put("role", toDto(saved));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating role: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id,
                                                          @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            Role role = roleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            String name = asString(payload.get("name"));
            String description = asString(payload.get("description"));
            @SuppressWarnings("unchecked")
            List<String> permissionsList = (List<String>) payload.get("permissions");

            if (name != null && !name.trim().isEmpty()) {
                String trimmed = name.trim();
                if (!trimmed.equals(role.getName()) && roleRepository.existsByName(trimmed)) {
                    response.put("success", false);
                    response.put("message", "Another role with this name already exists!");
                    return ResponseEntity.badRequest().body(response);
                }
                role.setName(trimmed);
            }

            if (description != null) {
                role.setDescription(description);
            }

            if (permissionsList != null) {
                String permissions = String.join(",", permissionsList);
                role.setPermissions(permissions);
            }

            Role saved = roleRepository.save(role);

            response.put("success", true);
            response.put("message", "Role updated successfully!");
            response.put("role", toDto(saved));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating role: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!roleRepository.existsById(id)) {
                response.put("success", false);
                response.put("message", "Role not found");
                return ResponseEntity.badRequest().body(response);
            }

            roleRepository.deleteById(id);

            response.put("success", true);
            response.put("message", "Role deleted successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting role: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, Object> toDto(Role role) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", role.getId());
        dto.put("name", role.getName());
        dto.put("description", role.getDescription());

        String permissions = role.getPermissions();
        if (permissions == null || permissions.isBlank()) {
            dto.put("permissions", List.of());
        } else {
            List<String> list = List.of(permissions.split("\\s*,\\s*"));
            dto.put("permissions", list);
        }

        return dto;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}

