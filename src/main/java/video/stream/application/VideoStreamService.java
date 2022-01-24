package video.stream.application;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import video.stream.application.exception.FileOperationException;
import video.stream.application.feign.VideoServiceFeign;

@Service
@Slf4j
public class VideoStreamService {

    protected static final String CONTENT_TYPE_PROPERTY = "video/mp4";
    protected static final String ACCEPT_RANGES_PROPERTY = "bytes";
    protected static final long INIT_BYTE_RANGE = 17000L;
    protected static final long BYTE_RANGE = 280000L;
    protected static final String WRONG_RANGE_ARG_ERROR_MSG = "The argument \"range\" is not valid.";
    private static final String BYTES_PREFIX = "bytes=";

    private final VideoServiceFeign videoServiceFeign;

    @Autowired
    public VideoStreamService(VideoServiceFeign videoServiceFeign) {
        this.videoServiceFeign = videoServiceFeign;
    }

    public ResponseEntity<byte[]> prepareContent(String range, Long videoId) throws FileOperationException {
        String filePath = getPath(videoId);
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return prepareContent(inputStream, range);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new FileOperationException();
        }
    }

    private String getPath(Long videoId) {
        return videoServiceFeign.getVideoFilePath(videoId).getPath();
    }

    private ResponseEntity<byte[]> prepareContent(InputStream inputStream, String range)
            throws IOException {
        Long fileSize = getFileSize(inputStream);
        String[] ranges = getRanges(range);
        long rangeStart = getRangeStart(ranges);
        long rangeEnd = getRangeEnd(ranges, rangeStart, fileSize);
        byte[] data = readByteRange(inputStream, rangeStart, rangeEnd);
        String contentLength = getContentLength(rangeStart, rangeEnd);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_PROPERTY)
                .header(HttpHeaders.ACCEPT_RANGES, ACCEPT_RANGES_PROPERTY)
                .header(HttpHeaders.CONTENT_LENGTH, contentLength)
                .header(HttpHeaders.CONTENT_RANGE, prepareContentRangeHeader(rangeStart, rangeEnd, fileSize))
                .body(data);
    }

    private Long getFileSize(InputStream inputStream) throws IOException {
        return Long.valueOf(inputStream.available());
    }

    private String[] getRanges(String range) {
        checkIfRangeHasValidPrefix(range);
        String[] ranges = range.replace(BYTES_PREFIX, "").split("-");
        checkIfRangesHaveValidLength(ranges);
        checkIfRangesHaveValidValues(ranges);
        return ranges;
    }

    private void checkIfRangeHasValidPrefix(String range) {
        if (!range.startsWith(BYTES_PREFIX)) {
            throw new IllegalArgumentException(WRONG_RANGE_ARG_ERROR_MSG);
        }
    }

    private void checkIfRangesHaveValidLength(String[] ranges) {
        if (ranges.length == 0 || ranges.length > 2) {
            throw new IllegalArgumentException(WRONG_RANGE_ARG_ERROR_MSG);
        }
    }

    private void checkIfRangesHaveValidValues(String[] ranges) {
        for (int i = 0; i < ranges.length; i++) {
            if (!StringUtils.isNumeric(ranges[i])) {
                throw new IllegalArgumentException(WRONG_RANGE_ARG_ERROR_MSG);
            }
        }
    }

    private long getRangeStart(String[] ranges) {
        return Long.parseLong(ranges[0]);
    }

    private long getRangeEnd(String[] ranges, long rangeStart, Long fileSize) {
        long rangeEnd;
        if (ranges.length > 1) {
            rangeEnd = Long.parseLong(ranges[1]);
        }
        if (rangeStart == 0) {
            rangeEnd = rangeStart + INIT_BYTE_RANGE;
        } else {
            rangeEnd = rangeStart + BYTE_RANGE;
        }
        if (fileSize < rangeEnd) {
            rangeEnd = fileSize - 1;
        }
        return rangeEnd;
    }

    private String getContentLength(long rangeStart, long rangeEnd) {
        return String.valueOf((rangeEnd - rangeStart) + 1);
    }

    private byte[] readByteRange(InputStream inputStream, long start, long end) throws IOException {
        inputStream.skipNBytes(start);
        return inputStream.readNBytes((int) end - (int) start + 1);
    }

    private String prepareContentRangeHeader(long rangeStart, long rangeEnd, Long fileSize) {
        return new StringBuilder()
                .append("bytes")
                .append(" ")
                .append(rangeStart)
                .append("-")
                .append(rangeEnd)
                .append("/")
                .append(fileSize)
                .toString();
    }
}
