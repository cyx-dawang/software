package com.health.modulea.controller;

import com.health.modulea.model.ActivityLevel;
import com.health.modulea.model.ApiException;
import com.health.modulea.model.Gender;
import com.health.modulea.model.HealthProfile;
import com.health.modulea.model.User;
import com.health.modulea.service.AccountService;
import com.health.modulea.service.HealthProfileService;
import com.health.modulea.service.PasswordHasher;
import com.health.modulea.store.InMemoryStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

public class ModuleAHttpServer {
    private final HttpServer server;
    private final AccountService accountService;
    private final HealthProfileService profileService;

    public ModuleAHttpServer(int port) throws IOException {
        InMemoryStore store = new InMemoryStore();
        this.accountService = new AccountService(store, new PasswordHasher());
        this.profileService = new HealthProfileService(store, accountService);
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", new Router());
    }

    public void start() {
        server.start();
    }

    private class Router implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String response = route(exchange);
                write(exchange, 200, response);
            } catch (ApiException e) {
                write(exchange, e.getStatusCode(), JsonUtil.error(e.getMessage()));
            } catch (Exception e) {
                write(exchange, 500, JsonUtil.error("服务器内部错误: " + e.getMessage()));
            }
        }

        private String route(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && "/health".equals(path)) {
                return JsonUtil.message("module-a is running");
            }
            if ("POST".equals(method) && "/auth/send-code".equals(path)) {
                Map<String, String> form = RequestUtil.form(exchange);
                String code = accountService.sendVerificationCode(RequestUtil.required(form, "mobile"));
                return JsonUtil.ok("{\"code\":\"" + code + "\"}");
            }
            if ("POST".equals(method) && "/auth/register".equals(path)) {
                Map<String, String> form = RequestUtil.form(exchange);
                User user = accountService.register(
                        RequestUtil.required(form, "mobile"),
                        RequestUtil.required(form, "code"),
                        RequestUtil.required(form, "password"),
                        RequestUtil.required(form, "nickname"));
                return JsonUtil.ok(JsonUtil.user(user));
            }
            if ("POST".equals(method) && "/auth/login".equals(path)) {
                Map<String, String> form = RequestUtil.form(exchange);
                User user = accountService.login(
                        RequestUtil.required(form, "mobile"),
                        RequestUtil.required(form, "password"));
                return JsonUtil.ok(JsonUtil.user(user));
            }
            if ("GET".equals(method) && path.startsWith("/users/")) {
                return JsonUtil.ok(JsonUtil.user(accountService.getUser(RequestUtil.pathId(path, "/users/"))));
            }
            if ("PUT".equals(method) && path.startsWith("/users/")) {
                Map<String, String> form = RequestUtil.form(exchange);
                User user = accountService.updateUserProfile(
                        RequestUtil.pathId(path, "/users/"),
                        form.get("nickname"),
                        form.get("avatarUrl"));
                return JsonUtil.ok(JsonUtil.user(user));
            }
            if ("GET".equals(method) && path.startsWith("/profiles/")) {
                return JsonUtil.ok(JsonUtil.profile(profileService.getProfile(RequestUtil.pathId(path, "/profiles/"))));
            }
            if ("PUT".equals(method) && path.startsWith("/profiles/")) {
                Map<String, String> form = RequestUtil.form(exchange);
                HealthProfile profile = profileService.saveProfile(
                        RequestUtil.pathId(path, "/profiles/"),
                        Gender.valueOf(RequestUtil.required(form, "gender")),
                        LocalDate.parse(RequestUtil.required(form, "birthDate")),
                        Integer.parseInt(RequestUtil.required(form, "heightCm")),
                        Double.parseDouble(RequestUtil.required(form, "weightKg")),
                        ActivityLevel.valueOf(RequestUtil.required(form, "activityLevel")));
                return JsonUtil.ok(JsonUtil.profile(profile));
            }
            throw new ApiException(404, "接口不存在");
        }

        private void write(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream output = exchange.getResponseBody();
            output.write(bytes);
            output.close();
        }
    }
}
