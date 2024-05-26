package fr.nnyoussef.webserver;

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
import java.util.concurrent.ConcurrentHashMap;

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

    private final ConcurrentHashMap<String, ResponseEntity<Flux<DataBuffer>>> cache = new ConcurrentHashMap<>(300);
    private final Map<String, HttpHeaders> httpHeadersMap;
    private final DefaultDataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

    @Value("${web.application.basepath}")
    String basePath;

    public WebServerRestController(Map<String, HttpHeaders> httpHeadersMap) {
        this.httpHeadersMap = httpHeadersMap;
    }

    @GetMapping("**")
    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(ServerWebExchange serverWebExchange) {
        String requestPath = serverWebExchange.getRequest().getPath().toString();
        ResponseEntity<Flux<DataBuffer>> response = cache.get(requestPath);

        if (response != null) {
            return just(cache.get(requestPath));
        }

        StringBuilder fileSystemPath = new StringBuilder(requestPath);
        if (requestPath.equals("/"))
            fileSystemPath.append("index.html");
        fileSystemPath.append(".br");

        Resource resource = new FileSystemResource(of(basePath, fileSystemPath.toString()));

        if (resource.exists()) {
            String finalPath = fileSystemPath.toString();
            return fromSupplier(() -> {
                String[] filePathContent = finalPath.split("[.]");
                String fileExtension = filePathContent[filePathContent.length - 2];
                ResponseEntity<Flux<DataBuffer>> responseEntity = ok()
                        .headers(httpHeadersMap.get(fileExtension))
                        .body(read(resource, dataBufferFactory, 10_048_576).cache());
                cache.put(requestPath, responseEntity);
                return responseEntity;
            }).subscribeOn(boundedElastic());
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/?redirected&path=".concat(requestPath));
            return just(new ResponseEntity<>(headers, PERMANENT_REDIRECT));
        }
    }

}
