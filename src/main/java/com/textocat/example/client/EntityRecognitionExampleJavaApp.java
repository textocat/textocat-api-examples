package com.textocat.example.client;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.textocat.example.client.Command.*;

/**
 * Textocat Entity Recognition Example in Java
 */
enum Command {
    QUEUE, REQUEST, RETRIEVE;

    public String toString() {
        return super.toString().toLowerCase();
    }
}

public class EntityRecognitionExampleJavaApp {
    private final static String authToken = "<YOUR_TOKEN_HERE>";
    private final static String requestUrl = "http://api.textocat.com/api/entity/";

    static HttpRequest prebuild(Command command) {
        HttpRequest request = command == QUEUE ? Unirest.post(requestUrl + command) : Unirest.get(requestUrl + command);
        return request.queryString("auth_token", authToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    static String queue(String text) throws UnirestException {
        String inputDocument = "[{\"text\": \"" + text + "\"}]";
        HttpResponse<JsonNode> response = ((HttpRequestWithBody) prebuild(QUEUE)).body(inputDocument).asJson();
        return response.getBody().getObject().getString("batchId");
    }

    static void waitUntilCompleted(String batchId) throws Exception {
        while (true) {
            Thread.sleep(1000);  // wait for 1 sec
            HttpResponse<JsonNode> response = prebuild(REQUEST).queryString("batch_id", batchId).asJson();
            if (response.getBody().getObject().getString("status").equals("FINISHED")) {
                break;
            }
        }
    }

    static JSONArray retrieve(String batchId) throws UnirestException {
        HttpResponse<JsonNode> response = prebuild(RETRIEVE).queryString("batch_id", batchId).asJson();
        return response.getBody().getObject().getJSONArray("documents");
    }

    public static void main(String[] args) throws Exception {
        String text = "Председатель совета директоров ОАО «МДМ Банк» Олег Вьюгин — о том," +
                " чему приведет обмен санкциями между Россией и Западом в следующем году. Беседовала Светлана Сухова.";
        String batchId = queue(text);
        waitUntilCompleted(batchId);
        JSONArray documents = retrieve(batchId);
        // output processed documents
        for (int i = 0; i < documents.length(); i++) {
            JSONArray entities = documents.getJSONObject(i).getJSONArray("entities");
            for (int j = 0; j < entities.length(); j++) {
                JSONObject entity = entities.getJSONObject(j);
                System.out.println(entity.getString("span") + ":" + entity.getString("category"));
            }
        }
    }
}
