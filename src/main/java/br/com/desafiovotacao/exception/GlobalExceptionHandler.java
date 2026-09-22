package br.com.desafiovotacao.exception;

import br.com.desafiovotacao.client.CpfInvalidoException;

import br.com.desafiovotacao.dto.ErroResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final Clock clock;

    @ExceptionHandler({PautaNaoEncontradaException.class, SessaoNaoEncontradaException.class,
            CpfInvalidoException.class})
    public ResponseEntity<Object> recursoNaoEncontrado(RuntimeException exception, WebRequest request) {
        return resposta(HttpStatus.NOT_FOUND, exception.getMessage(), request, HttpHeaders.EMPTY, Map.of());
    }

    @ExceptionHandler({VotoDuplicadoException.class, SessaoJaExistenteException.class,
            SessaoEncerradaException.class})
    public ResponseEntity<Object> conflito(RuntimeException exception, WebRequest request) {
        return resposta(HttpStatus.CONFLICT, exception.getMessage(), request, HttpHeaders.EMPTY, Map.of());
    }

    @ExceptionHandler(AssociadoNaoHabilitadoException.class)
    public ResponseEntity<Object> associadoNaoHabilitado(AssociadoNaoHabilitadoException exception,
            WebRequest request) {
        return resposta(HttpStatus.FORBIDDEN, exception.getMessage(), request, HttpHeaders.EMPTY, Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(erro ->
                fields.merge(erro.getField(), erro.getDefaultMessage() == null
                        ? "Valor inválido" : erro.getDefaultMessage(), (anterior, atual) -> anterior + "; " + atual));
        return resposta(status, "Erro de validação", request, headers, fields);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return resposta(status, "Corpo da requisição inválido", request, headers, Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("Falha inesperada ao processar requisição.", exception);
        }
        String message = status.is5xxServerError() ? "Ocorreu um erro interno inesperado."
                : status.value() == 400 ? "Parâmetros da requisição inválidos" : descricao(status);
        return resposta(status, message, request, headers, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> erroInesperado(Exception exception, WebRequest request) {
        log.error("Falha inesperada ao processar requisição.", exception);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado.",
                request, HttpHeaders.EMPTY, Map.of());
    }

    private ResponseEntity<Object> resposta(HttpStatusCode status, String message, WebRequest request,
            HttpHeaders headers, Map<String, String> fields) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErroResponse erro = new ErroResponse(LocalDateTime.now(clock), status.value(), descricao(status),
                message, path, fields);
        return new ResponseEntity<>(erro, headers, status);
    }

    private String descricao(HttpStatusCode status) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        return httpStatus == null ? "HTTP Error" : httpStatus.getReasonPhrase();
    }
}
