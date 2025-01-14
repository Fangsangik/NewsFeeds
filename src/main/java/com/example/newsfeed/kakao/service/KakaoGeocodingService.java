package com.example.newsfeed.kakao.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Service
public class KakaoGeocodingService {

    @Value("${kakao.client_id}")
    private String clientId;

    public String getAddress(double latitude, double longitude) {
        String url = String.format("https://dapi.kakao.com/v2/local/geo/coord2address.json?x=%s&y=%f", longitude, latitude);

        //HTTP Header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + clientId);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        //RestTemaplate로 API 호출
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        //JSON 파싱
        JSONObject jsonResponse = new JSONObject(response.getBody());
        if (!jsonResponse.getJSONArray("documents").isEmpty()) {
            return jsonResponse
                    .getJSONArray("documents")
                    .getJSONObject(0)
                    .getJSONObject("address")
                    .getString("address_name");
        } else {
            throw new IllegalArgumentException("주소를 찾을 수 없습니다.");
        }
    }


    /**
     * 주소를 입력받아 좌표를 반환
     * 사용 여부 고민중
     * @param address
     * @return
     */
    public double[] getCoordinates(String address) {
        String url = String.format("https://dapi.kakao.com/v2/local/search/address.json?query=%s", address);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + clientId);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        if (!jsonResponse.getJSONArray("documents").isEmpty()) {
            JSONObject coordinates = jsonResponse
                    .getJSONArray("documents")
                    .getJSONObject(0)
                    .getJSONObject("address");
            return new double[] {coordinates.getDouble("y"), coordinates.getDouble("x")};
        } else {
            throw new IllegalArgumentException("좌표를 찾을 수 없습니다.");
        }
    }
}
