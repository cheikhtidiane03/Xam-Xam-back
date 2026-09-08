package com.xaamxaam.service.auth;

import com.xaamxaam.config.SecuriteProperties;
import com.xaamxaam.exception.CompteTemporairementBloqueException;
import com.xaamxaam.repository.TentativeConnexionEchoueeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private TentativeConnexionEchoueeRepository repository;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        SecuriteProperties properties = new SecuriteProperties(5, 15);
        loginAttemptService = new LoginAttemptService(repository, properties);
    }

    @Test
    void verifierNonBloque_neLevePasSousLaLimite() {
        when(repository.countByEmailAndDateTentativeAfter(anyString(), any(LocalDateTime.class))).thenReturn(4L);

        assertThatCode(() -> loginAttemptService.verifierNonBloque("awa@exemple.com")).doesNotThrowAnyException();
    }

    @Test
    void verifierNonBloque_leveExceptionAuSeuil() {
        when(repository.countByEmailAndDateTentativeAfter(anyString(), any(LocalDateTime.class))).thenReturn(5L);

        assertThatThrownBy(() -> loginAttemptService.verifierNonBloque("awa@exemple.com"))
                .isInstanceOf(CompteTemporairementBloqueException.class);
    }
}
