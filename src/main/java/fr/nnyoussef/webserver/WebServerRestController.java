package fr.nnyoussef.webserver;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static java.lang.String.format;
import static java.net.URI.create;
import static java.nio.file.Path.of;
import static org.springframework.core.io.buffer.DataBufferUtils.read;
import static org.springframework.http.HttpStatus.PERMANENT_REDIRECT;
import static org.springframework.http.ResponseEntity.ok;
import static reactor.core.publisher.Mono.fromSupplier;
import static reactor.core.publisher.Mono.just;
import static reactor.core.scheduler.Schedulers.boundedElastic;

@Lazy
@RestController
public class WebServerRestController {

    private ImmutableMap<String, Mono<ResponseEntity<Flux<DataBuffer>>>> cache = ImmutableMap.of();
    private final Map<String, HttpHeaders> httpHeadersMap;
    private final DefaultDataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

    @Value("${web.application.basepath}")
    private String basePath;

    public WebServerRestController(Map<String, HttpHeaders> httpHeadersMap) {
        this.httpHeadersMap = httpHeadersMap;
    }

    @GetMapping("**")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(ServerWebExchange serverWebExchange) {
        String requestPath = serverWebExchange.getRequest().getPath().toString();
        Mono<ResponseEntity<Flux<DataBuffer>>> response = cache.get(requestPath);
        if (response != null)
            return response;

        StringBuilder fileSystemPath = new StringBuilder(requestPath);
        if (requestPath.equals("/"))
            fileSystemPath.append("index.html");
        fileSystemPath.append(".br");

        Resource resource = new FileSystemResource(of(basePath, fileSystemPath.toString()));
        if (resource.exists()) {
            return fromSupplier(() -> {
                String[] filePathContent = fileSystemPath.toString().split("[.]");
                String fileExtension = filePathContent[filePathContent.length - 2];
                ResponseEntity<Flux<DataBuffer>> responseEntity = ok()
                        .headers(httpHeadersMap.get(fileExtension))
                        .body(read(resource, dataBufferFactory, 10_048_576).cache());

                cache = ImmutableMap.<String, Mono<ResponseEntity<Flux<DataBuffer>>>>builder()
                        .putAll(cache)
                        .put(requestPath, just(responseEntity))
                        .build();
                return responseEntity;
            }).subscribeOn(boundedElastic()).cache();
        } else {
            Mono<ResponseEntity<Flux<DataBuffer>>> responseEntity = cache.get("/");
            if (responseEntity != null)
                return just(ok().headers(httpHeadersMap.get("html")).body(responseEntity.block().getBody()));
            else {
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(create(format("/?redirected&path=%s", requestPath)));
                return just(new ResponseEntity<>(headers, PERMANENT_REDIRECT));
            }
        }
    }
}
