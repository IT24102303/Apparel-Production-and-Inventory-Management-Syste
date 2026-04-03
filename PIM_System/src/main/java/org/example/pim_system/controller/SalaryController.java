package org.example.pim_system.controller;

import org.example.pim_system.model.Employee;
import org.example.pim_system.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/salary")
public class SalaryController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateSalary(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            String employeeId = (String) payload.get("employeeId");
            Double overtimeHours = payload.get("overtimeHours") != null
                    ? Double.valueOf(payload.get("overtimeHours").toString()) : 0.0;
            Double allowances = payload.get("allowances") != null
                    ? Double.valueOf(payload.get("allowances").toString()) : 0.0;
            Double advances = payload.get("advances") != null
                    ? Double.valueOf(payload.get("advances").toString()) : 0.0;

            if (employeeId == null || employeeId.isEmpty()) {
                response.put("success", false);
                response.put("message", "Employee ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Employee> optionalEmployee = employeeRepository.findByEmployeeId(employeeId);
            if (optionalEmployee.isEmpty()) {
                response.put("success", false);
                response.put("message", "Employee not found");
                return ResponseEntity.badRequest().body(response);
            }

            Employee employee = optionalEmployee.get();
            double basicSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : 0.0;

            // Simple OT calculation: hourly = basic / (8 hours * 26 working days), OT at 1.5x
            double hourlyRate = basicSalary / (8 * 26.0);
            double overtimeAmount = overtimeHours * hourlyRate * 1.5;

            double grossSalary = basicSalary + overtimeAmount + allowances;
            double netSalary = grossSalary - advances;

            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("basicSalary", basicSalary);
            breakdown.put("overtimeHours", overtimeHours);
            breakdown.put("overtimeAmount", overtimeAmount);
            breakdown.put("allowances", allowances);
            breakdown.put("advances", advances);
            breakdown.put("grossSalary", grossSalary);
            breakdown.put("netSalary", netSalary);

            response.put("success", true);
            response.put("employeeName", employee.getFullName());
            response.put("employeeId", employee.getEmployeeId());
            response.put("breakdown", breakdown);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error calculating salary: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/payslips")
    public ResponseEntity<Map<String, Object>> generatePayslips(@RequestParam(required = false) String month) {
        Map<String, Object> response = new HashMap<>();

        // Placeholder implementation: in a real system, you would generate PDFs or export files here.
        response.put("success", true);
        response.put("message", "Payslips generated successfully for " + (month != null ? month : "the selected period") + ".");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/payslips/download")
    public ResponseEntity<byte[]> downloadPayslips(@RequestParam(required = false) String month) {
        // Simple CSV generation for all employees as a placeholder payslip export
        List<Employee> employees = employeeRepository.findAll();

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Employee ID,Full Name,Position,Basic Salary (LKR)\n");
        for (Employee emp : employees) {
            csvBuilder
                    .append(emp.getEmployeeId() != null ? emp.getEmployeeId() : "").append(',')
                    .append(emp.getFullName() != null ? emp.getFullName() : "").append(',')
                    .append(emp.getPosition() != null ? emp.getPosition() : "").append(',')
                    .append(emp.getBasicSalary() != null ? emp.getBasicSalary() : 0.0)
                    .append('\n');
        }

        byte[] fileBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        String filename = "payslips-" + (month != null && !month.isEmpty() ? month : "current") + ".csv";
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }
}


