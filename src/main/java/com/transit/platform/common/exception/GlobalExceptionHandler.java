package com.transit.platform.common.exception;

import com.transit.platform.common.ApiErrorResponse;
import com.transit.platform.common.BusinessException;
import com.transit.platform.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Gestion centralisée des exceptions : toute erreur remontée au client suit la même
 * structure JSON, quel que soit le module d'origine.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.ofValidation("Les données fournies sont invalides", errors, request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(ErrorCode.FORBIDDEN, "Accès refusé", request.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(ErrorCode.UNAUTHORIZED, "Identifiants invalides", request.getRequestURI()));
    }

    /**
     * Correctif audit (Prompt 07 §36) : levée par Hibernate quand une mise à jour porte sur
     * une version obsolète (deux utilisateurs modifiant la même ressource simultanément).
     * Message explicite plutôt qu'une 500 générique — l'utilisateur peut recharger et réessayer.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(ErrorCode.CONCURRENT_MODIFICATION,
                        "Cette ressource a été modifiée entre-temps par un autre utilisateur. Veuillez recharger et réessayer.",
                        request.getRequestURI()));
    }

    /**
     * Correctif (retour utilisateur) : une contrainte de cohérence en base (dates, valeurs
     * uniques, clé étrangère...) remontait auparavant comme une erreur 500 générique et
     * incompréhensible ("Une erreur interne est survenue"). Spring traduit automatiquement
     * les exceptions JDBC/Hibernate en DataIntegrityViolationException dans la couche
     * Repository — c'est donc cette exception-là qu'il faut intercepter, pas l'exception
     * Hibernate d'origine (qui n'atteint jamais ce point, déjà enveloppée avant).
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Contrainte de base de données violée sur {} : {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR,
                        "Les informations saisies ne respectent pas une règle de cohérence (par exemple une date incohérente, ou une valeur déjà utilisée ailleurs). Vérifiez vos données et réessayez.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erreur non gérée sur {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Une erreur interne est survenue", request.getRequestURI()));
    }
}
