package com.allendowney.thinkdast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

import redis.clients.jedis.Jedis;

/**
 * Creates a Jedis connection safely.
 */
public class JedisMaker {

    /**
     * Make a Jedis object.
     */
    public static Jedis make() throws IOException {

        String slash = File.separator;
        String filename = "resources" + slash + "redis_url.txt";
        URL fileURL = JedisMaker.class.getClassLoader().getResource(filename);

        if (fileURL == null) {
            System.out.println("File not found: " + filename);
            printInstructions();
            return null;
        }

        String filepath = URLDecoder.decode(fileURL.getFile(), "UTF-8");

        BufferedReader br = new BufferedReader(new FileReader(filepath));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();

        URI uri = URI.create(sb.toString());

        String host = uri.getHost();
        int port = uri.getPort();

        Jedis jedis = new Jedis(host, port);

        // Authenticate ONLY if password exists
        String userInfo = uri.getUserInfo();
        if (userInfo != null && userInfo.contains(":")) {
            String[] parts = userInfo.split(":", 2);
            String password = parts.length > 1 ? parts[1] : null;

            if (password != null && !password.isEmpty()) {
                jedis.auth(password);
            }
        }

        return jedis;
    }

    private static void printInstructions() {
        System.out.println();
        System.out.println("Create a file called redis_url.txt in src/resources");
        System.out.println("For local Redis (no password), use:");
        System.out.println("redis://localhost:6379");
    }
}
