package com.example.demo;

import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.bootstrap-admin.username=admin",
        "app.bootstrap-admin.password=AdminPassword1!",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
class AuthenticationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void loginReplacesThePreviousSessionAndLogoutRevokesTheCurrentSession() throws Exception {
        String firstToken = login();
        String secondToken = login();

        createUser(firstToken, "old-session-user").andExpect(status().isUnauthorized());
        createUser(secondToken, "new-session-user")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.firstName").value("New"))
                .andExpect(jsonPath("$.data.lastName").value("Session-user"))
                .andExpect(jsonPath("$.data.roleName").value("EJECUTIVO"));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("ACCESS_TOKEN", secondToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        createUser(secondToken, "logged-out-user").andExpect(status().isUnauthorized());
    }

    @Test
    void userCreationRejectsInvalidPasswordsAndCaseInsensitiveDuplicateUsernames() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/v1/users")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"weak-password-user\",\"dni\":\"12345678\",\"firstName\":\"Weak\",\"lastName\":\"Password\",\"password\":\"password123\",\"role\":\"EJECUTIVO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password:")));

        createUser(token, "duplicate-user").andExpect(status().isCreated());
        createUser(token, "DUPLICATE-USER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El usuario ya se encuentra registrado."));
    }

    @Test
    void userCreationStoresDniAndRejectsDuplicates() throws Exception {
        String token = login();

        createUser(token, "dni-user", "EJECUTIVO", "12345678")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dni").value("12345678"));
        createUser(token, "other-dni-user", "EJECUTIVO", "12345678")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El DNI ya se encuentra registrado."));

        mockMvc.perform(get("/api/v1/users/dni/123")
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El DNI debe tener exactamente 8 digitos."));
    }

    @Test
    void failedLoginsAreCountedAndBlockTheAccountAfterThreeAttempts() throws Exception {
        String adminToken = login();
        createUser(adminToken, "locked-user").andExpect(status().isCreated());

        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"locked-user\",\"password\":\"WrongPassword1!\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"locked-user\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas."));

        var lockedUser = userRepository.findByUsername("locked-user").orElseThrow();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(lockedUser.isActive()).isFalse();
    }

    @Test
    void rolesAreAvailableToAdministrators() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/roles").cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].roleName").value("ADMIN"));
    }

    @Test
    void administratorsCanInactivateAUserInsteadOfDeletingIt() throws Exception {
        String token = login();
        createUser(token, "inactive-user").andExpect(status().isCreated());
        var user = userRepository.findByUsername("inactive-user").orElseThrow();

        mockMvc.perform(patch("/api/v1/users/{id}/status", user.getId())
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"inactive-user\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas."));
    }

    @Test
    void usersCanUpdateTheirOwnAccountOnlyAfterProvidingTheirCurrentPassword() throws Exception {
        String adminToken = login();
        createUser(adminToken, "account-user").andExpect(status().isCreated());
        String accountToken = login("account-user", "Password123!");

        MvcResult update = mockMvc.perform(put("/api/v1/auth/account")
                        .cookie(new Cookie("ACCESS_TOKEN", accountToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"account-user\",\"currentPassword\":\"Password123!\",\"newPassword\":\"UpdatedPassword1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("account-user"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("ACCESS_TOKEN")))
                .andReturn();
        String replacementToken = update.getResponse().getCookie("ACCESS_TOKEN").getValue();

        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie("ACCESS_TOKEN", accountToken)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie("ACCESS_TOKEN", replacementToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"account-user\",\"password\":\"UpdatedPassword1!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void passwordChangesRejectTheCurrentPasswordForAdministratorsAndUsers() throws Exception {
        String adminToken = login();
        createUser(adminToken, "password-user").andExpect(status().isCreated());
        var user = userRepository.findByUsername("password-user").orElseThrow();

        mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                        .cookie(new Cookie("ACCESS_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"password-user\",\"dni\":\"" + user.getDni()
                                + "\",\"firstName\":\"Password\",\"lastName\":\"User\",\"password\":\"Password123!\",\"roleId\":\""
                                + user.getRole().getId() + "\",\"active\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La nueva contraseña no puede ser igual a la actual."));

        String userToken = login("password-user", "Password123!");
        mockMvc.perform(put("/api/v1/auth/account")
                        .cookie(new Cookie("ACCESS_TOKEN", userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"password-user\",\"currentPassword\":\"Password123!\",\"newPassword\":\"Password123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La nueva contraseña no puede ser igual a la actual."));
    }

    @Test
    void administratorsCanUpdateTheApplicationNameAndLogo() throws Exception {
        mockMvc.perform(get("/api/v1/public/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationName").value("Conciertos"));

        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[] { 1, 2, 3 });
        mockMvc.perform(multipart("/api/v1/settings")
                        .file(logo)
                        .param("applicationName", "Eventos demo")
                        .cookie(new Cookie("ACCESS_TOKEN", login()))
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationName").value("Eventos demo"))
                .andExpect(jsonPath("$.data.hasLogo").value(true));

        mockMvc.perform(get("/api/v1/public/settings/logo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void administratorsCanFilterUsersByNameLastNameRoleAndStatus() throws Exception {
        String token = login();
        createUser(token, "filterable-user").andExpect(status().isCreated());
        var user = userRepository.findByUsername("filterable-user").orElseThrow();

        mockMvc.perform(get("/api/v1/users")
                        .queryParam("firstName", "Filterable")
                        .queryParam("lastName", "User")
                        .queryParam("roleId", user.getRole().getId().toString())
                        .queryParam("active", "true")
                        .queryParam("page", "0")
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].username").value("filterable-user"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void administratorsCanNavigateUserPages() throws Exception {
        String token = login();
        for (int index = 0; index < 21; index++) {
            createUser(token, "pagination-" + index).andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/users")
                        .queryParam("firstName", "Pagination")
                        .queryParam("page", "1")
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(21))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void executivesCanCreateOnlyTheirOwnProjectsWhileOtherRolesCanReadThem() throws Exception {
        String adminToken = login();
        createUser(adminToken, "project-executive", "EJECUTIVO").andExpect(status().isCreated());
        createUser(adminToken, "other-executive", "EJECUTIVO").andExpect(status().isCreated());
        createUser(adminToken, "project-manager", "GERENTE").andExpect(status().isCreated());
        var executive = userRepository.findByUsername("project-executive").orElseThrow();
        MvcResult company = mockMvc.perform(post("/api/v1/companies")
                        .cookie(new Cookie("ACCESS_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Empresa demo\",\"ruc\":\"20123456789\",\"address\":\"Av. Principal 123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("Av. Principal 123"))
                .andReturn();
        String companyId = com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.data.id");
        var storedCompany = companyRepository.findById(java.util.UUID.fromString(companyId)).orElseThrow();
        assertThat(storedCompany.getNameEncrypted()).doesNotContain("Empresa demo");
        assertThat(storedCompany.getRucEncrypted()).doesNotContain("20123456789");

        String executiveToken = login("project-executive", "Password123!");
        MvcResult created = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", executiveToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdById").value(executive.getId().toString()))
                .andExpect(jsonPath("$.data.companyName").value("Empresa demo"))
                .andExpect(jsonPath("$.data.companyRuc").value("20123456789"))
                .andReturn();

        String projectId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
        String otherExecutiveToken = login("other-executive", "Password123!");
        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .cookie(new Cookie("ACCESS_TOKEN", otherExecutiveToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId)))
                .andExpect(status().isForbidden());

        String managerToken = login("project-manager", "Password123!");
        mockMvc.perform(get("/api/v1/projects").cookie(new Cookie("ACCESS_TOKEN", managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id").value(org.hamcrest.Matchers.hasItem(projectId)));
    }

    @Test
    void projectsCanBeFilteredByExecutiveProjectCompanyDateApprovalAndStatus() throws Exception {
        String adminToken = login();
        createUser(adminToken, "filter-executive", "EJECUTIVO").andExpect(status().isCreated());
        MvcResult company = mockMvc.perform(post("/api/v1/companies")
                        .cookie(new Cookie("ACCESS_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Empresa filtros proyectos\",\"ruc\":\"20456789012\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String companyId = com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.data.id");

        String executiveToken = login("filter-executive", "Password123!");
        mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", executiveToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId).replace("Proyecto demo", "Proyecto filtros")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects")
                        .queryParam("search", "Proyecto filtros")
                        .queryParam("eventMonth", "2026-12")
                        .queryParam("approved", "PENDIENTE")
                        .queryParam("status", "VIGENTE")
                        .queryParam("page", "0")
                        .cookie(new Cookie("ACCESS_TOKEN", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].projectName").value("Proyecto filtros"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void projectsCanBeExportedToAnExcelFileForOneYear() throws Exception {
        String token = login();
        MvcResult company = mockMvc.perform(post("/api/v1/companies")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Empresa exportacion\",\"ruc\":\"20555555555\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String companyId = com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId).replace("2026", "2099")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId).replace("2026", "2027")))
                .andExpect(status().isOk());

        MvcResult export = mockMvc.perform(get("/api/v1/projects/export")
                        .queryParam("fromYear", "2099")
                        .queryParam("toYear", "2099")
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("proyectos-2099-2099.xlsx")))
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export.getResponse().getContentAsByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("EJECUTIVO");
            assertThat(sheet.getRow(0).getCell(11).getStringCellValue()).isEqualTo("% RENTABILIDAD");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Proyecto demo");
        }

        mockMvc.perform(get("/api/v1/projects/export")
                        .queryParam("fromYear", "2026")
                        .queryParam("toYear", "2031")
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedUsersCanViewEventDashboardForOneYear() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/projects/dashboard")
                        .queryParam("fromYear", "2100")
                        .queryParam("toYear", "2100")
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").value(0))
                .andExpect(jsonPath("$.data.executed").value(0))
                .andExpect(jsonPath("$.data.lost").value(0))
                .andExpect(jsonPath("$.data.cancelled").value(0))
                .andExpect(jsonPath("$.data.yearly.length()").value(1))
                .andExpect(jsonPath("$.data.yearly[0].year").value(2100));

        MvcResult company = mockMvc.perform(post("/api/v1/companies")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Empresa dashboard\",\"ruc\":\"20666666666\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String companyId = com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.data.id");
        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId)))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(project.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "SI", "VIGENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "SI", "EJECUTADO")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/dashboard")
                        .queryParam("fromYear", "2026")
                        .queryParam("toYear", "2026")
                        .queryParam("companyId", companyId)
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executed").value(1))
                .andExpect(jsonPath("$.data.yearly[0].executed").value(1));
    }

    @Test
    void projectWorkflowRecordsApprovalAndRejectionSteps() throws Exception {
        String token = login();
        MvcResult company = mockMvc.perform(post("/api/v1/companies")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Empresa flujo\",\"ruc\":\"20987654321\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String companyId = com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.data.id");

        MvcResult approvedProject = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId)))
                .andExpect(status().isOk())
                .andReturn();
        String approvedProjectId = com.jayway.jsonpath.JsonPath.read(approvedProject.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/v1/projects/{id}", approvedProjectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "SI", "VIGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value("SI"))
                .andExpect(jsonPath("$.data.status").value("VIGENTE"));

        mockMvc.perform(get("/api/v1/projects/{id}/history", approvedProjectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].previousApproval").value("PENDIENTE"))
                .andExpect(jsonPath("$.data[0].newApproval").value("SI"))
                .andExpect(jsonPath("$.data[0].previousStatus").value("VIGENTE"))
                .andExpect(jsonPath("$.data[0].newStatus").value("VIGENTE"));

        MvcResult rejectedProject = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId)))
                .andExpect(status().isOk())
                .andReturn();
        String rejectedProjectId = com.jayway.jsonpath.JsonPath.read(rejectedProject.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/v1/projects/{id}", rejectedProjectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "NO", "VIGENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value("NO"))
                .andExpect(jsonPath("$.data.status").value("VIGENTE"));

        mockMvc.perform(put("/api/v1/projects/{id}", rejectedProjectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "NO", "CANCELADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELADO"));

        mockMvc.perform(get("/api/v1/projects/{id}/history", rejectedProjectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].previousApproval").value("NO"))
                .andExpect(jsonPath("$.data[0].newApproval").value("NO"))
                .andExpect(jsonPath("$.data[0].previousStatus").value("VIGENTE"))
                .andExpect(jsonPath("$.data[0].newStatus").value("CANCELADO"))
                .andExpect(jsonPath("$.data[1].previousApproval").value("PENDIENTE"))
                .andExpect(jsonPath("$.data[1].newApproval").value("NO"))
                .andExpect(jsonPath("$.data[1].previousStatus").value("VIGENTE"))
                .andExpect(jsonPath("$.data[1].newStatus").value("VIGENTE"));

        mockMvc.perform(put("/api/v1/projects/{id}", rejectedProjectId)
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "NO", "PERDIDO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executivesCannotEditFinalizedProjectsButAdministratorsCan() throws Exception {
        String adminToken = login();
        createUser(adminToken, "final-executive").andExpect(status().isCreated());
        MvcResult company = mockMvc.perform(post("/api/v1/companies")
                        .cookie(new Cookie("ACCESS_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Empresa proyecto final\",\"ruc\":\"20777777777\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String companyId = com.jayway.jsonpath.JsonPath.read(company.getResponse().getContentAsString(), "$.data.id");

        String executiveToken = login("final-executive", "Password123!");
        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie("ACCESS_TOKEN", executiveToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(companyId)))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(project.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .cookie(new Cookie("ACCESS_TOKEN", executiveToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "SI", "VIGENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .cookie(new Cookie("ACCESS_TOKEN", executiveToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "SI", "EJECUTADO")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .cookie(new Cookie("ACCESS_TOKEN", executiveToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "SI", "EJECUTADO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .cookie(new Cookie("ACCESS_TOKEN", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectStateBody(companyId, "SI", "EJECUTADO")))
                .andExpect(status().isOk());
    }

    private ResultActions createUser(String token, String username) throws Exception {
        return createUser(token, username, "EJECUTIVO");
    }

    private ResultActions createUser(String token, String username, String role) throws Exception {
        return createUser(token, username, role, String.format("%08d", Math.floorMod(username.hashCode(), 100_000_000)));
    }

    private ResultActions createUser(String token, String username, String role, String dni) throws Exception {
        String[] name = username.split("-", 2);
        String firstName = capitalize(name[0]);
        String lastName = name.length > 1 ? capitalize(name[1]) : "User";
        return mockMvc.perform(post("/api/v1/users")
                        .cookie(new Cookie("ACCESS_TOKEN", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"dni\":\"" + dni + "\",\"firstName\":\"" + firstName
                                + "\",\"lastName\":\"" + lastName
                                + "\",\"password\":\"Password123!\",\"role\":\"" + role + "\"}"));
    }

    private String projectBody(String companyId) {
        return "{\"companyId\":\"" + companyId + "\",\"projectName\":\"Proyecto demo\","
                + "\"observations\":\"Observacion\",\"totalBudget\":1000.00,"
                + "\"year\":2026,\"eventDate\":\"2026-12-10\",\"tentativeBillingMonth\":\"2026-12\","
                + "\"finalBilling\":1200.00,\"costsWithoutVat\":800.00}";
    }

    private String projectStateBody(String companyId, String approved, String status) {
        return "{\"companyId\":\"" + companyId + "\",\"projectName\":\"Proyecto demo\","
                + "\"observations\":\"Observacion\",\"approved\":\"" + approved + "\",\"status\":\"" + status + "\","
                + "\"totalBudget\":1000.00,\"year\":2026,\"eventDate\":\"2026-12-10\","
                + "\"tentativeBillingMonth\":\"2026-12\",\"finalBilling\":1200.00,\"costsWithoutVat\":800.00}";
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String login() throws Exception {
        return login("admin", "AdminPassword1!");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"))))
                .andReturn();
        return result.getResponse().getCookie("ACCESS_TOKEN").getValue();
    }
}
