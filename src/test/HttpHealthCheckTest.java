package test;

import server.GameServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies that the server responds to standard HTTP GET /health requests with 200 OK
 * as required by Render's free tier health check monitor.
 */
public class HttpHealthCheckTest {
    private static final int HEALTH_PORT = 9876;

    public static void main(String[] args) {
        System.out.println("Testing HTTP Health Check Endpoint on port " + HEALTH_PORT + "...");
        GameServer server = new GameServer(HEALTH_PORT);
        Thread serverThread = new Thread(server::start, "HealthServer");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(600); // Wait for bind

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            // Test 1: GET /health
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + HEALTH_PORT + "/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            if (response.statusCode() != 200) {
                throw new AssertionError("Expected HTTP 200, got: " + response.statusCode());
            }
            if (!response.body().contains("\"status\":\"UP\"")) {
                throw new AssertionError("Response body does not contain UP status");
            }

            // Test 2: GET / (root endpoint)
            HttpRequest rootReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + HEALTH_PORT + "/"))
                    .GET()
                    .build();

            HttpResponse<String> rootResp = client.send(rootReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("Root Status Code: " + rootResp.statusCode());
            if (rootResp.statusCode() != 200) {
                throw new AssertionError("Expected HTTP 200 for root, got: " + rootResp.statusCode());
            }

            System.out.println("\n>>> HTTP HEALTH CHECK TESTS PASSED! RENDER COMPLIANCE VERIFIED! <<<");
        } catch (Exception e) {
            System.err.println("Health check failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            server.stop();
        }
    }
}
