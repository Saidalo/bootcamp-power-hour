package com.bootcamp.powerhour.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice(basePackages = "com.bootcamp.powerhour")
public class WebExceptionHandler {

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    public String handleNotFound(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        log.warn("Not found: {} {} ({})", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return errorPage(HttpStatus.NOT_FOUND, ex.getMessage(), response, model);
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        ResponseStatus annotated = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        if (annotated != null) {
            log.warn("{} on {} {}: {}", ex.getClass().getSimpleName(),
                    request.getMethod(), request.getRequestURI(), ex.getMessage());
            return errorPage(annotated.code(), ex.getMessage(), response, model);
        }
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again.", response, model);
    }

    private String errorPage(HttpStatus status, String message, HttpServletResponse response, Model model) {
        response.setStatus(status.value());
        model.addAttribute("status", status.value());
        model.addAttribute("error", status.getReasonPhrase());
        model.addAttribute("message", message);
        return "error";
    }
}
