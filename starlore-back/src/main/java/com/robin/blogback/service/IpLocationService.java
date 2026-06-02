package com.robin.blogback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class IpLocationService {

    private static final Logger log = LoggerFactory.getLogger(IpLocationService.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 查询 IP 归属地，返回 { country, province, city } */
    public IpLocation lookup(String ip) {
        if (ip == null || ip.isBlank() || ip.startsWith("127.") || ip.startsWith("192.168.") || "0:0:0:0:0:0:0:1".equals(ip)) {
            return new IpLocation("", "", "");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=status,country,regionName,city"))
                    .timeout(Duration.ofSeconds(3))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(resp.body());
                if ("success".equals(node.get("status").asText())) {
                    return new IpLocation(
                            node.get("country").asText(""),
                            node.get("regionName").asText(""),
                            node.get("city").asText("")
                    );
                }
            }
        } catch (Exception e) {
            log.warn("IP 归属地查询失败: {} ({})", ip, e.getMessage());
        }
        return new IpLocation("", "", "");
    }

    public record IpLocation(String country, String province, String city) {
        public String display() {
            if (country == null || country.isEmpty()) return "";
            if (province != null && !province.isEmpty() && !province.equals(country)) {
                return province + (city != null && !city.isEmpty() && !city.equals(province) ? " " + city : "");
            }
            return country;
        }
    }
}
