package video.stream.application.exception;

import org.springframework.http.HttpStatus;

public interface HttpException {

    String getMessage();

    HttpStatus getStatus();

}
