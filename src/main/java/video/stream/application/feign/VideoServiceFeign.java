package video.stream.application.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import video.stream.application.feign.dto.VideoPath;

@FeignClient(name = "video-service")
public interface VideoServiceFeign {

    @RequestMapping(method = RequestMethod.GET, value = "/video/path")
    VideoPath getVideoFilePath(@RequestParam Long videoId);
}
