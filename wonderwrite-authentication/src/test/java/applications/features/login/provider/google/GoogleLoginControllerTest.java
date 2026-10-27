package applications.features.login.provider.google;

import applications.domain.model.ClientFingerprint;
import applications.security.AuthCookieWriter;
import applications.security.AuthExceptionAdvice;
import applications.utils.tokens.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GoogleLoginControllerTest {

    @Mock
    private GoogleLoginService googleLoginService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GoogleLoginController(googleLoginService, new AuthCookieWriter()))
                .setControllerAdvice(new AuthExceptionAdvice())
                .build();
    }

    @Test
    void knownIdentityReturns200WithSessionCookies() throws Exception {
        UUID id = UUID.randomUUID();
        when(googleLoginService.login(any())).thenReturn(new GoogleLoginResult.Success(
                id, new TokenPair("access-value", "refresh-value", 900, 604800), false));

        MvcResult result = mockMvc.perform(post("/api/v1/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerUserId": "110483738369414381905",
                                  "email": "user@gmail.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.provisioned").value(false))
                .andReturn();

        assertThat(result.getResponse().getHeaders("Set-Cookie")).hasSize(2);
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("access_token=access-value", "Path=/", "HttpOnly", "Secure", "SameSite=Strict", "Max-Age=900");
    }

    @Test
    void unknownIdentityReturns401WithEmptyBodyAndNoCookies() throws Exception {
        when(googleLoginService.login(any())).thenReturn(
                new GoogleLoginResult.Failed("Unknown Google identity"));

        mockMvc.perform(post("/api/v1/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerUserId": "does-not-exist",
                                  "email": "ghost@gmail.com"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void loginCommandCarriesFingerprintFromRequestHeaders() throws Exception {
        when(googleLoginService.login(any())).thenReturn(
                new GoogleLoginResult.Failed("Unknown Google identity"));

        mockMvc.perform(post("/api/v1/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "curl/8.9.1")
                        .header("X-Forwarded-For", "198.51.100.7, 203.0.113.10")
                        .content("""
                                {
                                  "providerUserId": " 110483738369414381905 ",
                                  "email": "user@gmail.com"
                                }
                                """));

        ArgumentCaptor<GoogleLoginCommand> captor = ArgumentCaptor.forClass(GoogleLoginCommand.class);
        verify(googleLoginService).login(captor.capture());
        GoogleLoginCommand command = captor.getValue();

        assertThat(command.providerUserId()).isEqualTo("110483738369414381905");
        assertThat(command.fingerprint()).isEqualTo(new ClientFingerprint("curl/8.9.1", "198.51.100.7"));
    }

    @Test
    void blankProviderUserIdIsRejectedByBeanValidation() throws Exception {
        mockMvc.perform(post("/api/v1/login/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerUserId": "",
                                  "email": "user@gmail.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("VALIDATION_FAILED"));
    }
}
