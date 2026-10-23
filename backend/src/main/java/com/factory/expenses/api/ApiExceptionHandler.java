package com.factory.expenses.api;

import com.factory.expenses.domain.Category;
import com.factory.expenses.service.ExpenseNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private static final String VALIDATION = "/problems/validation-error";
  private static final String MALFORMED = "/problems/malformed-request";
  private static final String NOT_FOUND = "/problems/not-found";

  private static final String CATEGORY_MESSAGE =
      "must be one of "
          + Arrays.stream(Category.values()).map(Enum::name).collect(Collectors.joining(", "));

  record FieldProblem(String field, String message) {}

  @ExceptionHandler(ExpenseNotFoundException.class)
  ResponseEntity<Object> handleNotFound(ExpenseNotFoundException ex, HttpServletRequest request) {
    LOG.warn("{} {} -> 404 {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
    ProblemDetail problem = problem(HttpStatus.NOT_FOUND, NOT_FOUND, "Not found", ex.getMessage());
    problem.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(InvalidParameterException.class)
  ResponseEntity<Object> handleInvalidParameter(
      InvalidParameterException ex, HttpServletRequest request) {
    LOG.warn(
        "{} {} -> 400 invalid parameter {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getField());
    ProblemDetail problem =
        validationProblem(List.of(new FieldProblem(ex.getField(), ex.getMessage())), "parameter");
    problem.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
    LOG.error("{} {} -> 500", request.getMethod(), request.getRequestURI(), ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setTitle("Internal Server Error");
    problem.setInstance(URI.create(request.getRequestURI()));
    declareDefaultType(problem);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail problem) {
      declareDefaultType(problem);
    }
    return super.createResponseEntity(body, headers, statusCode, request);
  }

  /**
   * Jackson omits the default "about:blank" type, but the contract's Problem schema requires the
   * member, so framework-generated problems get an explicit per-status type.
   */
  private static void declareDefaultType(ProblemDetail problem) {
    URI type = problem.getType();
    if (type == null || "about:blank".equals(type.toString())) {
      problem.setType(URI.create("/problems/http-" + problem.getStatus()));
    }
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldProblem> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(ApiExceptionHandler::toFieldProblem)
            .toList();
    return respond(validationProblem(errors, "field"), headers, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST,
            MALFORMED,
            "Malformed request",
            "Request body could not be read");
    fieldOf(ex).ifPresent(f -> problem.setProperty("errors", List.of(f)));
    return respond(problem, headers, request);
  }

  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    FieldProblem error = new FieldProblem(ex.getParameterName(), "is required");
    return respond(validationProblem(List.of(error), "parameter"), headers, request);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String name = ex.getPropertyName() == null ? "value" : ex.getPropertyName();
    if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
      name = mismatch.getName();
    }
    Class<?> required = ex.getRequiredType();
    String message =
        required != null && required.isAssignableFrom(Category.class)
            ? CATEGORY_MESSAGE
            : "must be a valid " + (required == null ? "value" : required.getSimpleName());
    if (required != null && Number.class.isAssignableFrom(wrap(required))) {
      message = "must be a number";
    }
    return respond(
        validationProblem(List.of(new FieldProblem(name, message)), "parameter"), headers, request);
  }

  private static Class<?> wrap(Class<?> type) {
    return type == long.class ? Long.class : type;
  }

  private ResponseEntity<Object> respond(
      ProblemDetail problem, HttpHeaders headers, WebRequest request) {
    LOG.warn("{} -> {} {}", request.getDescription(false), problem.getStatus(), problem.getTitle());
    return handleExceptionInternal(
        new IllegalArgumentException(problem.getDetail()),
        problem,
        headers,
        HttpStatus.valueOf(problem.getStatus()),
        request);
  }

  private static ProblemDetail validationProblem(List<FieldProblem> errors, String noun) {
    int n = errors.size();
    String detail = "Request has " + n + " invalid " + noun + (n == 1 ? "" : "s");
    ProblemDetail problem =
        problem(HttpStatus.BAD_REQUEST, VALIDATION, "Validation failed", detail);
    problem.setProperty("errors", errors);
    return problem;
  }

  private static ProblemDetail problem(
      HttpStatus status, String type, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(type));
    problem.setTitle(title);
    return problem;
  }

  private static FieldProblem toFieldProblem(FieldError error) {
    return new FieldProblem(error.getField(), error.getDefaultMessage());
  }

  private static java.util.Optional<FieldProblem> fieldOf(HttpMessageNotReadableException ex) {
    if (ex.getCause() instanceof MismatchedInputException mismatch
        && !mismatch.getPath().isEmpty()) {
      String field = mismatch.getPath().getLast().getPropertyName();
      if (field == null) {
        return java.util.Optional.empty();
      }
      boolean isCategory = mismatch instanceof InvalidFormatException && "category".equals(field);
      return java.util.Optional.of(
          new FieldProblem(field, isCategory ? CATEGORY_MESSAGE : "has an invalid value"));
    }
    return java.util.Optional.empty();
  }
}
