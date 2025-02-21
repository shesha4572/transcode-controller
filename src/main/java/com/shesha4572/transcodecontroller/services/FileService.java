package com.shesha4572.transcodecontroller.services;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

@Service
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
            System.out.println("Failed to fetch file. Status: " + response.getStatusCode());
        }
        return null;
    }
}
