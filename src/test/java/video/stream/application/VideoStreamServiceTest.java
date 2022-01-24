package video.stream.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import video.stream.application.exception.FileOperationException;
import video.stream.application.feign.VideoServiceFeign;
import video.stream.application.feign.dto.VideoPath;

@ExtendWith(MockitoExtension.class)
public class VideoStreamServiceTest {

    @InjectMocks
    private VideoStreamService videoStreamService;

    @Mock
    private VideoServiceFeign videoServiceFeign;

    private final static String FILE_PATH = "src/test/resources/testVideoFile.mp4";
    private final static Long FILE_SIZE = 56733888L;

    private void mockFilePath() {
        when(videoServiceFeign.getVideoFilePath(Mockito.anyLong()))
                .thenReturn(new VideoPath(FILE_PATH));
    }

    @BeforeEach
    public void setUp() {
        mockFilePath();
    }

    @Test
    void shouldPrepareContentForInitRange() throws FileOperationException {
        ResponseEntity<byte[]> response = videoStreamService.prepareContent("bytes=0-", 1L);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody().length)
                .isEqualTo(VideoStreamService.INIT_BYTE_RANGE + 1);
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo(VideoStreamService.CONTENT_TYPE_PROPERTY);
        assertThat(response.getHeaders().get(HttpHeaders.ACCEPT_RANGES).get(0).toString())
                .isEqualTo(VideoStreamService.ACCEPT_RANGES_PROPERTY.toString());
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0).toString())
                .isEqualTo(String.valueOf(VideoStreamService.INIT_BYTE_RANGE + 1));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_RANGE).get(0).toString())
                .isEqualTo("bytes 0-" + VideoStreamService.INIT_BYTE_RANGE + "/" + FILE_SIZE);
    }

    @Test
    void shouldPrepareContentForMiddleRange() throws FileOperationException {
        ResponseEntity<byte[]> response = videoStreamService.prepareContent("bytes=178000-", 1L);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody().length)
                .isEqualTo(VideoStreamService.BYTE_RANGE + 1);
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo(VideoStreamService.CONTENT_TYPE_PROPERTY);
        assertThat(response.getHeaders().get(HttpHeaders.ACCEPT_RANGES).get(0).toString())
                .isEqualTo(VideoStreamService.ACCEPT_RANGES_PROPERTY.toString());
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0).toString())
                .isEqualTo(String.valueOf(VideoStreamService.BYTE_RANGE + 1));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_RANGE).get(0).toString())
                .isEqualTo("bytes 178000-" + (VideoStreamService.BYTE_RANGE + 178000) + "/" + FILE_SIZE);
    }

    @Test
    void shouldPrepareContentForLastRange() throws FileOperationException {
        final long lastRange = FILE_SIZE - VideoStreamService.BYTE_RANGE + 1000;

        ResponseEntity<byte[]> response = videoStreamService.prepareContent("bytes=" + lastRange + "-", 1L);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody().length)
                .isEqualTo(FILE_SIZE - lastRange);
        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo(VideoStreamService.CONTENT_TYPE_PROPERTY);
        assertThat(response.getHeaders().get(HttpHeaders.ACCEPT_RANGES).get(0).toString())
                .isEqualTo(VideoStreamService.ACCEPT_RANGES_PROPERTY.toString());
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0).toString())
                .isEqualTo(String.valueOf(FILE_SIZE - lastRange));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_RANGE).get(0).toString())
                .isEqualTo("bytes " + lastRange + "-" + (FILE_SIZE - 1) + "/" + FILE_SIZE);
    }

    @Test
    void shouldThrowExceptionIfRangeHasInvalidPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> videoStreamService.prepareContent("bes=0-", 1L),
                VideoStreamService.WRONG_RANGE_ARG_ERROR_MSG);
    }

    @Test
    void shouldThrowExceptionIfRangeHasInvalidRangeValues1() {
        assertThrows(IllegalArgumentException.class,
                () -> videoStreamService.prepareContent("bytes=", 1L),
                VideoStreamService.WRONG_RANGE_ARG_ERROR_MSG);
    }

    @Test
    void shouldThrowExceptionIfRangeHasInvalidRangeValues2() {
        assertThrows(IllegalArgumentException.class,
                () -> videoStreamService.prepareContent("bytes=asdw-", 1L),
                VideoStreamService.WRONG_RANGE_ARG_ERROR_MSG);
    }

    @Test
    void shouldThrowExceptionIfRangeHasInvalidRangeValues3() {
        assertThrows(IllegalArgumentException.class,
                () -> videoStreamService.prepareContent("bytes=-", 1L),
                VideoStreamService.WRONG_RANGE_ARG_ERROR_MSG);
    }

    @Test
    void shouldThrowExceptionIfRangeHasInvalidRangeValues4() {
        assertThrows(IllegalArgumentException.class,
                () -> videoStreamService.prepareContent("bytes=123-123-123", 1L),
                VideoStreamService.WRONG_RANGE_ARG_ERROR_MSG);
    }
}
