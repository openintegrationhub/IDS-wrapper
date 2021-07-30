package de.fraunhofer.iais.eis.oihplus.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fraunhofer.iais.eis.oihplus.config.ConfigLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
@EnableConfigurationProperties( ConfigLogger.class)
public class DSCService {

        @Autowired
        private ConfigLogger logger;

    private static String BODY_TEMPLATE = "{\"data\":{\"filter\":%s},\"metadata\":{}}";

    public void callHook(String flowID,  List<String> filter) {


        var headers = new LinkedMultiValueMap<String, String>();
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        var body = getBody(filter);

        headers.setAll(map);

        HttpEntity<?> request = new HttpEntity<>(body, headers);
        new RestTemplate().postForEntity(flowID, request, String.class);

        if (logger.enabled) {
            log.info(String.format("Starting flow with body %s", body));}

    }

    private String getBody( List<String> filterList ) {
        return String.format(BODY_TEMPLATE,(filterList.toString()));

    }

}
