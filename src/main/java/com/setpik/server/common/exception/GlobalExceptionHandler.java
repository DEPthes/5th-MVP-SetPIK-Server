package com.setpik.server.common.exception;

import com.setpik.server.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
		log.error("BusinessException 발생: errorCode={}", exception.getErrorCode(), exception);   
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus())
			.body(ApiResponse.failure(errorCode, null));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<FieldErrorDetail>> handleValidationException(
		MethodArgumentNotValidException exception
	) {
		FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.orElse(null);
		FieldErrorDetail detail = fieldError == null
			? new FieldErrorDetail("request", ErrorCode.INVALID_REQUEST.getMessage())
			: new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());

		return ResponseEntity.badRequest()
			.body(ApiResponse.failure(ErrorCode.INVALID_REQUEST, detail));
	}

	@ExceptionHandler({
		ConstraintViolationException.class,
		HttpMessageNotReadableException.class,
		MissingServletRequestParameterException.class,
		MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
		return ResponseEntity.badRequest()
			.body(ApiResponse.failure(ErrorCode.INVALID_REQUEST, null));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
		return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
			.body(ApiResponse.failure(ErrorCode.RESOURCE_NOT_FOUND, null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
		log.error("Unhandled server exception", exception);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
			.body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR, null));
	}
}
