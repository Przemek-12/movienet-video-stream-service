package video.stream.application.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FileOperationException extends IOException implements HttpException {

    private static final long serialVersionUID = 1L;

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @Override
    public String getMessage() {
        return "File could not be read properly.";
    }

}
