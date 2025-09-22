package exception;

import java.util.List;
import java.util.ArrayList;


public class ValidationException extends GameException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(message);
    }

    public ValidationException(String message, List<String> validationErrors) {
        super(message, "VALIDATION_ERROR");
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public ValidationException(String message, String errorCode, List<String> validationErrors) {
        super(message, errorCode);
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public ValidationException(String message, String errorCode, List<String> validationErrors, Object context) {
        super(message, errorCode, context);
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }

    public void addValidationError(String error) {
        validationErrors.add(error);
    }

    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ValidationException{errorCode='%s', message='%s', errors=%s}",
                getErrorCode(), getMessage(), validationErrors);
    }
}


