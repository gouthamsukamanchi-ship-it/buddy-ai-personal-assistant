package com.buddy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIService {

    private final HttpClient client;

    public AIService() {

        client = HttpClient.newHttpClient();

        System.out.println("======================================");
        System.out.println("       BUDDY AI SERVICE READY");
        System.out.println("       Ollama Connected");
        System.out.println("======================================");
    }

    public String ask(String question) {

        if (question == null || question.trim().isEmpty()) {
            return "Yes, Sir. What's on your mind today?";
        }

        try {

            String escapedQuestion =
                    question
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", " ");

            String json =
                    "{"
                            + "\"model\":\"llama3.2\","
                            + "\"stream\":false,"
                            + "\"messages\":["
                            + "{"
                            + "\"role\":\"system\","
                            + "\"content\":\"You are Buddy, a friendly personal voice assistant. Answer naturally and briefly. Call the user Sir. Do not use markdown.\""
                            + "},"
                            + "{"
                            + "\"role\":\"user\","
                            + "\"content\":\"" + escapedQuestion + "\""
                            + "}"
                            + "]"
                            + "}";

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "http://localhost:11434/api/chat"
                            ))
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            json
                                    )
                            )
                            .build();

            System.out.println();
            System.out.println("======================================");
            System.out.println("          ASKING OLLAMA");
            System.out.println("======================================");

            System.out.println("USER: " + question);

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "Ollama HTTP Code: "
                            + response.statusCode()
            );

            if (response.statusCode() != 200) {

                System.out.println(
                        "Ollama Error: "
                                + response.body()
                );

                return "Sorry, Sir. I couldn't connect to my local AI.";
            }

            String body = response.body();

            System.out.println();
            System.out.println("OLLAMA RESPONSE:");
            System.out.println(body);

            String answer =
                    extractContent(body);

            if (
                    answer == null
                            || answer.trim().isEmpty()
            ) {

                return "Sorry, Sir. I couldn't think of an answer.";
            }

            System.out.println();
            System.out.println("======================================");
            System.out.println("          BUDDY RESPONSE");
            System.out.println("======================================");

            System.out.println(answer);

            return answer.trim();

        } catch (Exception e) {

            System.out.println();
            System.out.println("======================================");
            System.out.println("          OLLAMA ERROR");
            System.out.println("======================================");

            e.printStackTrace();

            return "Sorry, Sir. I'm having trouble connecting to my local AI.";
        }
    }

    private String extractContent(String json) {

        String key = "\"content\":\"";

        int start = json.indexOf(key);

        if (start < 0) {
            return "";
        }

        start += key.length();

        StringBuilder result =
                new StringBuilder();

        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {

            char c = json.charAt(i);

            if (escaped) {

                if (c == 'n') {
                    result.append('\n');
                } else if (c == 'r') {
                    result.append('\r');
                } else if (c == 't') {
                    result.append('\t');
                } else {
                    result.append(c);
                }

                escaped = false;

            } else if (c == '\\') {

                escaped = true;

            } else if (c == '"') {

                break;

            } else {

                result.append(c);
            }
        }

        return result.toString();
    }
}