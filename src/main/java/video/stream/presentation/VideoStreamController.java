package video.stream.presentation;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import video.stream.application.VideoStreamService;

@RestController
@RequestMapping("/video-stream")
public class VideoStreamController {

    private final VideoStreamService videoStreamService;

    @Autowired
    public VideoStreamController(VideoStreamService videoStreamService) {
        this.videoStreamService = videoStreamService;
    }

    @GetMapping
    public ResponseEntity<byte[]> get(@RequestHeader(HttpHeaders.RANGE) String range, @RequestParam Long videoId)
            throws IOException {
        return videoStreamService.prepareContent(range, videoId);
    }

}
