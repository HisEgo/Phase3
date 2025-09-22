package exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleException {

    String level() default "SEVERE";


    boolean rethrow() default false;


    String action() default "";


    String message() default "";


    int maxRetries() default 3;


    long retryDelay() default 1000;
}


