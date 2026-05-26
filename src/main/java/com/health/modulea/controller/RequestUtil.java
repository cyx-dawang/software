package com.health.modulea.controller;

import com.health.modulea.model.ApiException;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class RequestUtil {
    private RequestUtil() {
    }

    public static Map<String, String> form(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, String> values = new HashMap<String, String>();
        if (body.trim().isEmpty()) {
            return values;
        }
        String[] parts = body.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = decode(pair[0]);
            String value = pair.length > 1 ? decode(pair[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    public static String required(Map<String, String> form, String name) {
        String value = form.get(name);
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(400, name + "不能为空");
        }
        return value.trim();
    }

    public static long pathId(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            throw new ApiException(404, "接口不存在");
        }
        String id = path.substring(prefix.length());
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "路径ID格式错误");
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream input = exchange.getRequestBody();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (IOException e) {
            throw new ApiException(400, "参数编码错误");
        }
    }
}
