package fr.nnyoussef.webserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.core.io.buffer.DataBufferUtils.read;
import static org.springframework.http.ResponseEntity.ok;
import static reactor.core.scheduler.Schedulers.boundedElastic;

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

        String path = serverWebExchange.getRequest().getPath().toString();

        if (path.equals("/"))
            path = path.concat("index.html");
        path = path.concat(".br");

        if (cache.containsKey(path)) {
            return Mono.just(cache.get(path));
        }

        Resource resource = new FileSystemResource(Path.of(basePath, path));

        if (resource.exists()) {
            String finalPath = path;
            return Mono.fromSupplier(() -> {
                String[] filePathContent = finalPath.split("[.]");
                String fileExtension = filePathContent[filePathContent.length - 2];
                ResponseEntity<Flux<DataBuffer>> responseEntity = ok()
                        .headers(httpHeadersMap.get(fileExtension))
                        .body(read(resource, dataBufferFactory, 10_048_576));
                cache.put(finalPath, responseEntity);
                return responseEntity;
            }).subscribeOn(boundedElastic());
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/");
            return Mono.just(new ResponseEntity<>(headers, HttpStatus.NOT_FOUND));
        }
    }

}
