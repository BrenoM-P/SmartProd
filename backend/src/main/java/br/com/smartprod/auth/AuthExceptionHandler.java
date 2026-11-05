package br.com.smartprod.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erros do login em RFC 7807. A mensagem é a mesma para senha errada, e-mail inexistente e usuário
 * inativo, para não revelar quais e-mails existem.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
class AuthExceptionHandler {

    @ExceptionHandler({ BadCredentialsException.class, AccountStatusException.class })
    ProblemDetail credenciaisInvalidas() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "E-mail ou senha inválidos.");
        problema.setTitle("Credenciais inválidas");
        return problema;
    }
}
