package fr.nnyoussef.webserver;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.*;

@Configuration
public class WebServerConfiguration {

    @Bean()
    public Map<String, HttpHeaders> httpHeadersMap() {
        return ImmutableMap.<String, HttpHeaders>builder()
                .put("html", getHttpHeaders(TEXT_HTML))
                .put("js", getHttpHeaders(parseMediaType("text/javascript")))
                .put("webp", getHttpHeaders(parseMediaType("image/webp")))
                .put("json", getHttpHeaders(APPLICATION_JSON))
                .put("ico", getHttpHeaders(parseMediaType("image/x-icon")))
                .put("jpeg", getHttpHeaders(IMAGE_JPEG))
                .put("png", getHttpHeaders(IMAGE_PNG))
                .put("gif", getHttpHeaders(IMAGE_GIF))
                .put("txt", getHttpHeaders(TEXT_PLAIN))
                .put("xml", getHttpHeaders(TEXT_XML))
                .put("pdf", getHttpHeaders(APPLICATION_PDF))
                .put("css", getHttpHeaders(parseMediaType("text/css")))
                .build();
    }

    private HttpHeaders getHttpHeaders(MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set("Content-Encoding", "br");
        headers.setCacheControl(maxAge(365, DAYS));
        return headers;
    }
}
