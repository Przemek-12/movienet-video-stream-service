package video.stream.application;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import video.stream.application.exception.FileOperationException;
import video.stream.application.feign.VideoServiceFeign;

@Service
public class VideoStreamService {

    private final String CONTENT_TYPE_PROPERTY = "video/mp4";
    private final String ACCEPT_RANGES_PROPERTY = "bytes";
    private final long INIT_BYTE_RANGE = 17000L;
    private final long BYTE_RANGE = 280000L;

    private final VideoServiceFeign videoServiceFeign;

    @Autowired
    public VideoStreamService(VideoServiceFeign videoServiceFeign) {
        this.videoServiceFeign = videoServiceFeign;
    }

    public ResponseEntity<byte[]> prepareContent(String range, Long videoId) throws FileOperationException {
        String filePath = getPath(videoId);
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath))) {
            return prepareContent(inputStream, range);
        } catch (IOException e) {
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
        return range.replace("bytes=", "").split("-");
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
