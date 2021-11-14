package video.stream.application;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import video.stream.application.feign.VideoServiceFeign;

@Service
public class VideoStreamService {

    private final String CONTENT_TYPE_PROPERTY = "video/mp4";
    private final String ACCEPT_RANGES_PROPERTY = "bytes";

    private final VideoServiceFeign videoServiceFeign;

    @Autowired
    public VideoStreamService(VideoServiceFeign videoServiceFeign) {
        this.videoServiceFeign = videoServiceFeign;
    }

//  String VIDEO_PATH = "C:\\Users\\przem\\Videos\\Free YouTube Downloader\\MassEffect.mp4";
//    String VIDEO_PATH = "C:\\Users\\przem\\Videos\\Free YouTube Downloader\\Mass Effect 2 Soundtrack - Suicide Mission [Extended].mp4";
//  String VIDEO_PATH = "C:\\Users\\przem\\Desktop\\pierdoły\\betrayal\\VID_20210227_192913.mp4";


    public ResponseEntity<byte[]> prepareContent(String range, Long videoId) throws IOException {

        FileInputStream fis = getFileInputStream(videoId);
        Long fileSize = getFileSize(fis);
        String[] ranges = getRanges(range);

        long rangeStart = getRangeStart(ranges);
        long rangeEnd = getRangeEnd(ranges, rangeStart, fileSize);
        byte[] data = readByteRange(fis, rangeStart, rangeEnd);

        String contentLength = getContentLength(rangeStart, rangeEnd);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_PROPERTY)
                .header(HttpHeaders.ACCEPT_RANGES, ACCEPT_RANGES_PROPERTY)
                .header(HttpHeaders.CONTENT_LENGTH, contentLength)
                .header(HttpHeaders.CONTENT_RANGE, prepareContentRangeHeader(rangeStart, rangeEnd, fileSize))
                .body(data);
    }

    private FileInputStream getFileInputStream(Long videoId) throws FileNotFoundException {
        return new FileInputStream(getPath(videoId));
    }

    private String getPath(Long videoId) {
        return videoServiceFeign.getVideoFilePath(videoId).getPath();
    }

    private Long getFileSize(FileInputStream fis) throws IOException {
        return Long.valueOf(fis.available());
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
            rangeEnd = rangeStart + 17000;
        } else {
            rangeEnd = rangeStart + 280000;
        }
        if (fileSize < rangeEnd) {
            rangeEnd = fileSize - 1;
        }
        return rangeEnd;
    }

    private String getContentLength(long rangeStart, long rangeEnd) {
        return String.valueOf((rangeEnd - rangeStart) + 1);
    }

    private byte[] readByteRange(FileInputStream fis, long start, long end) throws IOException {
        fis.skipNBytes(start);
        byte[] result = fis.readNBytes((int) end - (int) start + 1);
        fis.close();
        return result;
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
