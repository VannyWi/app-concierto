package com.example.demo.controller;
import com.example.demo.dto.request.CompanyRequest;
import com.example.demo.dto.request.UpdateCompanyStatusRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CompanyResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.RucLookupResponse;
import com.example.demo.service.CompanyService;
import com.example.demo.service.RucLookupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/companies") @RequiredArgsConstructor public class CompanyController {
 private final CompanyService companies;
 private final RucLookupService rucLookup;
 @GetMapping public ApiResponse<PageResponse<CompanyResponse>> findAll(@RequestParam(required = false) String name, @RequestParam(required = false) String ruc, @RequestParam(required = false) Boolean active, @RequestParam(defaultValue = "0") int page) { return new ApiResponse<>(true, "Companies retrieved", companies.findAll(name, ruc, active, page)); }
 @GetMapping("/ruc/{ruc}") @PreAuthorize("hasAnyRole('ADMIN', 'EJECUTIVO')") public ApiResponse<RucLookupResponse> lookupRuc(@PathVariable String ruc) { return new ApiResponse<>(true, "RUC consultado", rucLookup.lookup(ruc)); }
 @PostMapping @PreAuthorize("hasAnyRole('ADMIN', 'EJECUTIVO')") public ApiResponse<CompanyResponse> create(@Valid @RequestBody CompanyRequest request, Authentication auth) { return new ApiResponse<>(true, "Company created", companies.create(request, auth.getName())); }
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN', 'EJECUTIVO')") public ApiResponse<CompanyResponse> update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request, Authentication auth) { return new ApiResponse<>(true, "Company updated", companies.update(id, request, auth.getName())); }
 @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN', 'EJECUTIVO')") public ApiResponse<CompanyResponse> updateStatus(@PathVariable UUID id, @RequestBody UpdateCompanyStatusRequest request, Authentication auth) { return new ApiResponse<>(true, "Company status updated", companies.updateStatus(id, request.active(), auth.getName())); }
}
