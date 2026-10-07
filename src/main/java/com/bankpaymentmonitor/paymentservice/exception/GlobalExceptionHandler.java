package com.bankpaymentmonitor.paymentservice.exception;

import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentAlreadyExistsException;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import com.bankpaymentmonitor.paymentservice.payment.exception.InvalidPaymentStatusTransitionException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException exception
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "PAYMENT_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }
    @ExceptionHandler(PaymentAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentAlreadyExists(
            PaymentAlreadyExistsException exception
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "PAYMENT_ALREADY_EXISTS",
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "VALIDATION_ERROR");
        response.put("message", "La requête contient des données invalides");
        response.put("fields", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    @ExceptionHandler(InvalidPaymentStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPaymentStatusTransition(
            InvalidPaymentStatusTransitionException exception
    ) {

        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "INVALID_PAYMENT_STATUS_TRANSITION",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {

        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PAYMENT_STATUS",
                "Invalid payment status: " + exception.getValue()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }
}