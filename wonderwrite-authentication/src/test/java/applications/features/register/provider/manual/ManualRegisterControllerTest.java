package applications.features.register.provider.manual;

import applications.security.AuthExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ManualRegisterControllerTest {

    @Mock
    private ManualRegisterService manualRegisterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ManualRegisterController(manualRegisterService))
                .setControllerAdvice(new AuthExceptionAdvice())
                .build();
    }

    @Test
    void validRegistrationReturns201WithCreatedAccount() throws Exception {
        UUID id = UUID.randomUUID();
        when(manualRegisterService.execute(any())).thenReturn(
                new ManualRegisterResult.Success(id, "newuser@example.com", false));

        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newuser@example.com",
                                  "password": "S3cret!passphrase",
                                  "confirmPassword": "S3cret!passphrase"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.emailVerification").value(false));
    }

    @Test
    void duplicateEmailReturns409WithEmptyBody() throws Exception {
        when(manualRegisterService.execute(any())).thenReturn(
                new ManualRegisterResult.Failed("Email is already registered"));

        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "taken@example.com",
                                  "password": "S3cret!passphrase",
                                  "confirmPassword": "S3cret!passphrase"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string(""));
    }

    @Test
    void malformedEmailReturns400ValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "S3cret!passphrase",
                                  "confirmPassword": "S3cret!passphrase"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    void blankPasswordReturns400ValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newuser@example.com",
                                  "password": "",
                                  "confirmPassword": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("VALIDATION_FAILED"));
    }

    @Test
    void passwordMismatchIsMappedTo400InsteadOfCrashing() throws Exception {
        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newuser@example.com",
                                  "password": "S3cret!passphrase",
                                  "confirmPassword": "different-passphrase"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Password confirmation does not match"));
    }
}
