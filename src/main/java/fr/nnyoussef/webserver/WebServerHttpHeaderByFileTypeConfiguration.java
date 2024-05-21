package fr.nnyoussef.webserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.springframework.http.MediaType.*;

@Configuration
public class WebServerHttpHeaderByFileTypeConfiguration {

    @Bean()
    public Map<String, HttpHeaders> get() {
        return Map.ofEntries(
                new SimpleEntry<>("html", getHttpHeaders(TEXT_HTML)),
                new SimpleEntry<>("js", getHttpHeaders(parseMediaType("text/javascript"))),
                new SimpleEntry<>("webp", getHttpHeaders(parseMediaType("image/webp"))),
                new SimpleEntry<>("json", getHttpHeaders(APPLICATION_JSON)),
                new SimpleEntry<>("css", getHttpHeaders(parseMediaType("text/css"))));
    }

    private HttpHeaders getHttpHeaders(MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set("Content-Encoding", "br");
        headers.setCacheControl(CacheControl.maxAge(365, DAYS));
        return headers;
    }
}
