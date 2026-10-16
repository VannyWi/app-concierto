package com.example.demo.service;
import com.example.demo.dto.request.CompanyRequest;
import com.example.demo.dto.response.CompanyResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.entity.CompanyEntity;
import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor public class CompanyService {
  private final CompanyRepository companies; private final UserRepository users; private final CompanyDataProtector protector;
  public PageResponse<CompanyResponse> findAll(String name, String ruc, Boolean active, int page) { List<CompanyResponse> filtered = companies.findAll().stream().map(this::response).filter(company -> contains(company.name(), name) && contains(company.ruc(), ruc) && (active == null || company.active() == active)).sorted(Comparator.comparing(CompanyResponse::name, String.CASE_INSENSITIVE_ORDER)).toList(); int from = Math.min(page * 20, filtered.size()); int to = Math.min(from + 20, filtered.size()); return new PageResponse<>(filtered.subList(from, to), page, 20, filtered.size(), (int) Math.ceil((double) filtered.size() / 20)); }
   @Transactional public CompanyResponse create(CompanyRequest request, String username) { String rucHash = protector.rucHash(request.ruc()); if (companies.findByRucHash(rucHash).isPresent()) throw ApiException.conflict("El RUC ya se encuentra registrado."); CompanyEntity company = new CompanyEntity(); company.setCreatedBy(actor(username)); copy(company, request, rucHash); return response(companies.save(company)); }
   @Transactional public CompanyResponse update(UUID id, CompanyRequest request, String username) { CompanyEntity company = permitted(id, username); String rucHash = protector.rucHash(request.ruc()); companies.findByRucHash(rucHash).filter(found -> !found.getId().equals(id)).ifPresent(found -> { throw ApiException.conflict("El RUC ya se encuentra registrado."); }); copy(company, request, rucHash); return response(company); }
 @Transactional public CompanyResponse updateStatus(UUID id, boolean active, String username) { CompanyEntity company = permitted(id, username); company.setActive(active); return response(company); }
  private CompanyEntity permitted(UUID id, String username) { UserEntity actor = actor(username); CompanyEntity company = companies.findById(id).orElseThrow(() -> ApiException.notFound("Empresa no encontrada.")); if (!admin(actor) && !company.getCreatedBy().getId().equals(actor.getId())) throw ApiException.forbidden("No tienes permiso para modificar esta empresa."); return company; }
   private void copy(CompanyEntity company, CompanyRequest request, String rucHash) { String name = request.name().trim(); String ruc = request.ruc(); String address = request.address() == null || request.address().isBlank() ? null : request.address().trim(); company.setNameEncrypted(protector.encrypt(name)); company.setRucEncrypted(protector.encrypt(ruc)); company.setAddressEncrypted(protector.encrypt(address)); company.setRucHash(rucHash); }
  private UserEntity actor(String username) { return users.findByUsername(UserService.normalizeUsername(username)).orElseThrow(() -> ApiException.unauthorized("Se requiere autenticación.")); }
 private boolean admin(UserEntity user) { return Role.ADMIN.name().equals(user.getRole().getRoleName()); }
  private boolean contains(String value, String filter) { return filter == null || filter.isBlank() || value.toLowerCase(Locale.ROOT).contains(filter.trim().toLowerCase(Locale.ROOT)); }
  private CompanyResponse response(CompanyEntity company) { CompanyDataProtector.CompanyData data = protector.decrypt(company); return new CompanyResponse(company.getId(), data.name(), data.ruc(), data.address(), company.isActive()); }
}
