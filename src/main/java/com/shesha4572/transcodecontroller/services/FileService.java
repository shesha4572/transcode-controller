package com.shesha4572.transcodecontroller.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.io.File;

@Service
@Slf4j
public class FileService {

    private final RestTemplate restTemplate;

    private final String FS_API_URL = "http://localhost:6800";

    public FileService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] downloadFile(String id){
        String url = FS_API_URL + "/read/" + id;

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, null, byte[].class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            log.info("Failed to fetch file. Status: {}", response.getStatusCode());
        }
        return null;
    }

    public boolean uploadFile(File file, String fileId, String fileName) {
        String url = FS_API_URL + "/upload";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("file_id", fileId);
        body.add("file_name", fileName);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<?> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("File {} uploaded successfully! Response: {}" , fileName , response.getBody());
            return true;
        } else {
            log.info("Failed to upload file {}. Status: {}" , fileName , response.getStatusCode());
        }
        return false;
    }
}

