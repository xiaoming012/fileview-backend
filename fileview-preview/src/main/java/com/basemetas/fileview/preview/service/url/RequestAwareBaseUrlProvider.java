/*
 * Copyright 2025 BaseMetas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.basemetas.fileview.preview.service.url;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从请求上下文中解析 baseUrl 的提供者。
 * <p>
 * 依赖 {@code server.forward-headers-strategy=native} 配置，
 * Tomcat RemoteIpValve 会自动将 X-Forwarded-Proto/Host 等代理头
 * 映射到 request 对象，因此本类优先使用 request 原生方法获取 scheme/host/port。
 * <p>
 * 额外保留以下防御性措施以应对代理配置不完整或 Docker 端口映射场景：
 * <ul>
 *     <li>Origin/Referer 补偿：检测同主机端口不一致（Docker -p 映射）或内部地址回退（代理未传递 Host）</li>
 *     <li>X-Forwarded-Port 补偿：RemoteIpValve 不处理此头，需手动检查</li>
 *     <li>scheme-port 交叉校验：修正 https+80 或 http+443 等不一致组合</li>
 * </ul>
 * 解析失败时回退到配置的 baseUrl。
 */
@Component
public class RequestAwareBaseUrlProvider implements BaseUrlProvider {
    private static final Logger logger = LoggerFactory.getLogger(RequestAwareBaseUrlProvider.class);

    @Value("${fileview.preview.url.base-url}")
    private String baseUrl;

    @Override
    public String getBaseUrl() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // native 策略下，request 已自动反映 X-Forwarded-Proto/Host
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();

                logger.debug("📊 request基础值 - scheme: {}, serverName: {}, serverPort: {}", scheme, serverName, serverPort);

                // 调试日志：查看原始请求头（用于排查代理配置问题）
                String origin = request.getHeader("Origin");
                String referer = request.getHeader("Referer");
                logger.debug("📊 请求头调试 - Origin: {}, Referer: {}, Host: {}, X-Forwarded-Host: {}, X-Forwarded-Proto: {}",
                        origin, referer, request.getHeader("Host"),
                        request.getHeader("X-Forwarded-Host"), request.getHeader("X-Forwarded-Proto"));

                // 防御性补偿：从 Origin/Referer 检测并修正地址/端口不一致
                // Origin 代表浏览器实际访问的地址，可修正以下场景：
                // 1. Docker 端口映射（如 -p 9000:80）导致外部端口丢失
                // 2. 代理未正确传递 Host/Port 导致使用容器内部地址
                String clientUrl = (origin != null && !origin.trim().isEmpty()) ? origin
                        : (referer != null && !referer.trim().isEmpty()) ? referer : null;
                if (clientUrl != null) {
                    try {
                        java.net.URI uri = new java.net.URI(clientUrl);
                        if (uri.getHost() != null) {
                            int clientPort = uri.getPort() == -1 ? getDefaultPort(uri.getScheme()) : uri.getPort();
                            boolean shouldOverride = false;

                            if (uri.getHost().equalsIgnoreCase(serverName)) {
                                // 同主机、端口不同 → Docker 端口映射场景（如 -p 9000:80）
                                shouldOverride = (clientPort != serverPort);
                            } else if (isInternalAddress(serverName)) {
                                // 不同主机、服务端为内部地址 → 代理未传递真实 Host
                                shouldOverride = true;
                            }

                            if (shouldOverride) {
                                String source = (origin != null && !origin.trim().isEmpty()) ? "Origin" : "Referer";
                                serverName = uri.getHost();
                                serverPort = clientPort;
                                if (uri.getScheme() != null) {
                                    scheme = uri.getScheme();
                                }
                                logger.debug("🌐 从 {} 补偿 - ServerName: {}, ServerPort: {}, Scheme: {}",
                                        source, serverName, serverPort, scheme);
                            }
                        }
                    } catch (Exception e) {
                        String source = (origin != null && !origin.trim().isEmpty()) ? "Origin" : "Referer";
                        logger.warn("⚠️ {} 解析失败: {}", source, clientUrl, e);
                    }
                }

                // scheme 与 port 交叉校验修正（防御代理头配置不一致）
                // 补偿 X-Forwarded-Port：RemoteIpValve 不处理此头，需手动检查
                String forwardedPort = request.getHeader("X-Forwarded-Port");
                if (forwardedPort != null && !forwardedPort.trim().isEmpty()) {
                    try {
                        int fwdPort = Integer.parseInt(forwardedPort.trim());
                        if (fwdPort != serverPort) {
                            logger.debug("🔧 X-Forwarded-Port({}) 与 serverPort({}) 不一致，以代理头为准", fwdPort, serverPort);
                            serverPort = fwdPort;
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略无效的端口值
                    }
                }
                if (serverPort == 443 && "http".equals(scheme)) {
                    scheme = "https";
                    logger.debug("🔧 端口443修正协议为https");
                } else if (serverPort == 80 && "https".equals(scheme)) {
                    serverPort = 443;
                    logger.debug("🔧 https协议修正端口为443");
                }

                // 优先从 X-Forwarded-Prefix 请求头获取 contextPath
                String headerPrefix = request.getHeader("X-Forwarded-Prefix");
                String requestContextPath = request.getContextPath();
                String contextPath;

                if (headerPrefix != null && !headerPrefix.trim().isEmpty()) {
                    contextPath = normalizeContextPath(headerPrefix);
                    logger.info("🌐 使用 X-Forwarded-Prefix - HeaderPrefix: {}, Normalized: {}, RequestContextPath: {}",
                            headerPrefix, contextPath, requestContextPath);
                } else {
                    contextPath = normalizeContextPath(requestContextPath);
                    logger.debug("🌐 使用 request.getContextPath - ContextPath: {}, Normalized: {}",
                            requestContextPath, contextPath);
                }

                StringBuilder url = new StringBuilder();
                url.append(scheme).append("://").append(serverName);

                // 只在非标准端口时添加端口号
                if (("http".equals(scheme) && serverPort != 80) ||
                        ("https".equals(scheme) && serverPort != 443)) {
                    url.append(":").append(serverPort);
                }

                // 上下文路径不为空且不是根路径时追加
                if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
                    url.append(contextPath);
                }

                String dynamicUrl = url.toString();
                logger.info("✅ 动态生成baseUrl - Result: {}", dynamicUrl);
                return dynamicUrl;
            }
        } catch (Exception e) {
            logger.warn("⚠️ 无法从请求中获取baseUrl，使用配置值: {}", baseUrl, e);
        }

        // 回退：使用配置的baseUrl
        return baseUrl;
    }

    /**
     * 判断是否为内部/容器地址（代理未正确传递 Host 时 request 会返回这些值）
     */
    private boolean isInternalAddress(String host) {
        return host == null
                || "localhost".equalsIgnoreCase(host)
                || host.startsWith("127.")
                || host.startsWith("10.")
                || host.startsWith("172.")
                || host.startsWith("192.168.")
                || host.startsWith("0:");
    }

    /**
     * 根据协议返回默认端口
     */
    private int getDefaultPort(String scheme) {
        return "https".equals(scheme) ? 443 : 80;
    }

    private String normalizeContextPath(String raw) {
        if (raw == null) {
            return "";
        }
        String ctx = raw.trim().replace("\\", "/");
        if (ctx.isEmpty()) {
            return "";
        }
        if ("/".equals(ctx)) {
            return "/";
        }
        if (!ctx.startsWith("/")) {
            ctx = "/" + ctx;
        }
        // 压缩多余的斜杠
        ctx = ctx.replaceAll("/+", "/");
        // 去掉尾部斜杠（保留根路径）
        if (ctx.length() > 1 && ctx.endsWith("/")) {
            ctx = ctx.substring(0, ctx.length() - 1);
        }
        return ctx;
    }
}
