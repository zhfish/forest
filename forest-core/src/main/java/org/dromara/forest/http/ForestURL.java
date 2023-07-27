/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jun Gong
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.dromara.forest.http;

import org.dromara.forest.exceptions.ForestRuntimeException;
import org.dromara.forest.utils.StringUtils;
import org.dromara.forest.utils.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Forest URL
 *
 * @author gongjun[dt_flys@hotmail.com]
 * @since v1.5.2
 */
public class ForestURL implements Cloneable {

    private final static Logger log = LoggerFactory.getLogger(ForestURL.class);

    /**
     * 原始的 URL
     * <p>原始的，未被加工过的 URL 字符串，含有字符串模板表达式</p>
     */
    private String originalUrl;

    /**
     * 已生成的 URL
     * <p>最终输出的 URL 字符串
     */
    private String generatedUrl;

    private ForestAddress address;

    /**
     * HTTP协议
     */
    private String scheme;

    /**
     * 主机地址
     */
    private RequestVariable<String> host = new RequestVariable<>();

    /**
     * 主机端口
     */
    private RequestVariable<Integer> port = new RequestVariable<>();

    /**
     * URL根路径
     */
    private RequestVariable<String> basePath = new RequestVariable<>();

    /**
     * URL路径
     * <p>该路径为整个URL去除前面协议 + Host + Port 后部分
     */
    private RequestVariable<String> path = new RequestVariable<>();

    /**
     * 用户信息
     *
     * <p>包含在URL中的用户信息，比如:
     * <p>URL http://xxx:yyy@localhost:8080 中 xxx:yyy 的部分为用户信息
     * <p>其中，xxx为用户名，yyy为用户密码
     */
    private String userInfo;

    /**
     * reference
     * <p>URL井号(#)后面的字符串
     */
    private String ref;

    /**
     * 是否为SSL
     */
    private boolean ssl;

    /**
     * 是否需要重新生成 URL
     */
    private boolean needRegenerateUrl = false;



    private void needRegenerateUrl() {
        needRegenerateUrl = true;
    }

    public ForestURL(URL url) {
        if (url == null) {
            throw new ForestRuntimeException("[Forest] Request url cannot be null!");
        }
        setScheme(url.getProtocol());
        host.set(url.getHost());
        port.set(url.getPort());
        path.set(url.getPath());
        userInfo = url.getUserInfo();
        setRef(url.getRef());
    }

    public ForestURL(String scheme, String userInfo, String host, Integer port, String path) {
        this(scheme, userInfo, host, port, path, null);
    }


    public ForestURL(String scheme, String userInfo, String host, Integer port, String path, String ref) {
        setScheme(scheme);
        this.userInfo = userInfo;
        this.host.set(host);
        this.port.set(port);
        this.path.set(path);
        this.ref = ref;
        needRegenerateUrl();
    }

    /**
     * 获取原始URL
     *
     * @return 原始URL字符串
     */
    public String getGeneratedUrl() {
        if (generatedUrl == null || needRegenerateUrl) {
            generatedUrl = toURLString();
            needRegenerateUrl = false;
        }
        return generatedUrl;
    }

    /**
     * 设置基础地址信息
     *
     * @param baseAddress {@link ForestAddress}对象
     * @return {@link ForestURL}对象
     */
    public ForestURL setBaseAddress(ForestAddress baseAddress) {
        if (baseAddress != null) {
            final String baseScheme = baseAddress.getScheme();
            final String baseHost = baseAddress.getHost();
            final String bastPath = baseAddress.getBasePath();

            final int basePort = baseAddress.getPort();
            setBasePath(bastPath);
            setScheme(baseScheme);
            setHost(baseHost);
            if (basePort != -1) {
                setPort(basePort);
            }
        }
        return this;
    }

    public String getScheme() {
        if (StringUtils.isEmpty(scheme) && address != null) {
            return address.getScheme();
        }
        if (StringUtils.isEmpty(scheme)) {
            return ssl ? "https" : "http";
        }
        return scheme;
    }

    private void refreshSSL() {
        this.ssl = "https".equals(this.scheme);
    }

    public ForestURL setScheme(String scheme) {
        if (StringUtils.isBlank(scheme)) {
            return this;
        }
        this.scheme = scheme.trim();
        refreshSSL();
        needRegenerateUrl();
        return this;
    }

    public String getHost() {
        if (StringUtils.isEmpty(host.get()) && address != null) {
            return address.getHost();
        }
        return host.get();
    }

    public RequestVariable<String> hostVariable() {
        return host;
    }

    public ForestURL setHost(String host) {
        if (StringUtils.isBlank(host)) {
            return this;
        }
        this.host.set(host.trim());
        final String hostStr = this.host.get();
        if (hostStr.endsWith("/")) {
            this.host.set(hostStr.substring(0, hostStr.lastIndexOf("/")));
        }
        needRegenerateUrl();
        return this;
    }

    private static int normalizePort(Integer port, boolean ssl) {
        if (URLUtils.isNonePort(port)) {
            return ssl ? 443 : 80;
        }
        return port;
    }

    public int getPort() {
        if (URLUtils.isNonePort(port.get()) && address != null) {
            return normalizePort(address.getPort(), ssl);
        }
        return normalizePort(port.get(), ssl);
    }

    public ForestURL setPort(int port) {
        this.port.set(port);
        needRegenerateUrl();
        return this;
    }

    public RequestVariable<Integer> portVariable() {
        return this.port;
    }

    /**
     * 获取URL根路径
     * <p>该路径为整个URL去除前面协议 + Host + Port 后部分
     *
     * @return URL根路径
     */
    public String normalizeBasePath() {
        if (StringUtils.isEmpty(basePath.get())) {
            return normalizeBasePath(address.getBasePath());
        }
        return normalizeBasePath(basePath.get());
    }


    private String normalizeBasePath(String basePath) {
        if (StringUtils.isNotEmpty(basePath) && basePath.charAt(0) != '/') {
            return '/' + basePath;
        }
        return basePath;
    }


    /**
     * 设置URL根路径 (强制修改)
     * <p>该路径为整个URL去除前面协议 + Host + Port 后部分
     *
     * @param basePath 根路径
     * @return {@link ForestURL}对象实例
     */
    public ForestURL setBasePath(String basePath) {
        return setBasePath(basePath, true);
    }

    /**
     * 设置URL根路径
     * <p>该路径为整个URL去除前面协议 + Host + Port 后部分
     *
     * @param basePath 根路径
     * @param forced 是否强制修改, {@code true}: 强制修改非根路径部分地址信息, {@code false}: 非强制，如果URL已设置host、port等非根路径部分地址信息则不会修改
     * @return {@link ForestURL}对象实例
     */
    public ForestURL setBasePath(String basePath, boolean forced) {
        if (basePath == null) {
            return this;
        }
        String basePathStr = basePath.trim();
        if (!basePathStr.startsWith("/")) {
            if (URLUtils.isURL(basePathStr)) {
                try {
                    final String originHost = this.host.get();
                    final URL url = new URL(basePathStr);
                    if (forced || StringUtils.isEmpty(this.scheme)) {
                        setScheme(url.getProtocol());
                    }
                    if (forced || StringUtils.isEmpty(this.userInfo)) {
                        this.userInfo = url.getUserInfo();
                    }
                    if (forced || StringUtils.isEmpty(this.host.get())) {
                        this.host.set(url.getHost());
                    }
                    if (forced || (URLUtils.isNonePort(port.get()) && StringUtils.isEmpty(originHost))) {
                        this.port.set(url.getPort());
                    }
                    this.basePath.set(url.getPath());
                } catch (MalformedURLException e) {
                    throw new ForestRuntimeException(e);
                }
            } else {
                this.basePath.set("/" + this.basePath.get());
            }
        }
        needRegenerateUrl();
        return this;
    }

    public RequestVariable<String> basePath() {
        return this.basePath;
    }

    /**
     * 获取URL路径
     * <p>该路径为整个URL去除前面协议 + Host + Port + BasePath 后部分
     *
     * @return URL路径
     */
    public String getPath() {
        final String pathStr = path.get();
        if (StringUtils.isNotEmpty(pathStr) && pathStr.charAt(0) != '/') {
            return '/' + pathStr;
        }
        return pathStr;
    }

    /**
     * 设置URL路径
     * <p>该路径为整个URL去除前面协议 + Host + Port + BasePath 后部分
     *
     * @param path URL路径
     * @return {@link ForestURL}对象实例
     */
    public ForestURL setPath(String path) {
        if (path == null) {
            return this;
        }
        this.path.set(path.trim());
        needRegenerateUrl();
        return this;
    }

    public RequestVariable<String> pathVariable() {
        return this.path;
    }

    public String getUserInfo() {
        if (StringUtils.isEmpty(userInfo) && address != null) {
            return address.getUserInfo();
        }
        return userInfo;
    }

    public ForestURL setUserInfo(String userInfo) {
        this.userInfo = userInfo;
        needRegenerateUrl();
        return this;
    }

    public String getAuthority() {
        final StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(userInfo)) {
            builder.append(URLUtils.userInfoEncode(userInfo, "UTF-8")).append("@");
        }
        if (StringUtils.isNotEmpty(host.get())) {
            builder.append(URLUtils.userInfoEncode(host.get(), "UTF-8"));
        }
        final Integer portInt = port.get();
        if (URLUtils.isNotNonePort(portInt) &&
                ((portInt != 80 && portInt != 443 && portInt > -1) ||
                (portInt == 80 && !ssl) ||
                (portInt == 443 && !ssl))) {
            builder.append(':').append(portInt);
        }
        return builder.toString();
    }

    public String getRef() {
        return ref;
    }

    public ForestURL setRef(String ref) {
        this.ref = ref;
        return this;
    }

    public boolean isSSL() {
        if (StringUtils.isEmpty(scheme) && address != null) {
            return "https".equals(address.getScheme());
        }
        return ssl;
    }

    public String toURLString() {
        final StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(scheme)) {
            builder.append(scheme).append("://");
        }
        final String authority = getAuthority();
        if (StringUtils.isNotEmpty(authority)) {
            builder.append(authority);
        }
        if (StringUtils.isNotEmpty(basePath.get())) {
            String encodedBasePath = URLUtils.pathEncode(basePath.get(), "UTF-8");
            if (host.get() != null && encodedBasePath.charAt(0) != '/') {
                builder.append('/');
            }
            builder.append(encodedBasePath);
        }
        if (StringUtils.isNotEmpty(path.get())) {
            String encodedPath = URLUtils.pathEncode(path.get(), "UTF-8");
            if ((host.get() != null || basePath != null) && encodedPath.charAt(0) != '/') {
                builder.append('/');
            }
            builder.append(encodedPath);
        }
        return builder.toString();
    }

    /**
     * 获取URL对应的路由
     *
     * @return {@link ForestRoute}对象实例
     * @author gongjun [dt_flys@hotmail.com]
     * @since 1.5.22
     */
    public ForestRoute getRoute() {
        return ForestRoutes.getRoute(getHost(), getPort());
    }

    @Override
    public String toString() {
        if (StringUtils.isNotEmpty(ref)) {
            return getGeneratedUrl() + "#" + ref;
        }
        return getGeneratedUrl();
    }



    public URL toJavaURL() {
        try {
            return new URL(getGeneratedUrl());
        } catch (MalformedURLException e) {
            throw new ForestRuntimeException(e);
        }
    }

    public URI toURI() {
        try {
            return new URI(getGeneratedUrl());
        } catch (URISyntaxException e) {
            throw new ForestRuntimeException(e);
        }
    }

    /**
     * 修改地址信息 (强制修改)
     *
     * @param address 地址, {@link ForestAddress}对象实例
     * @return {@link ForestURL}对象实例
     */
    public ForestURL setAddress(ForestAddress address) {
        return setAddress(address, true);
    }

    /**
     * 修改地址信息
     *
     * @param address 地址, {@link ForestAddress}对象实例
     * @param forced 是否强制修改, {@code true}: 强制修改, {@code false}: 非强制，如果URL已设置host、port等信息则不会修改
     * @return {@link ForestURL}对象实例
     */
    public ForestURL setAddress(ForestAddress address, boolean forced) {
        if (forced) {
            setBaseAddress(address);
        } else {
            this.address = address;
        }
        return this;
    }


    /**
     * 合并两个URL
     * @param url 被合并的一个URL
     * @return 合并完的新URL
     */
    public ForestURL mergeURLWith(ForestURL url) {
        if (url == null) {
            return this;
        }
        String newSchema = this.scheme == null ? url.scheme : this.scheme;
        String newUserInfo = this.userInfo == null ? url.userInfo : this.userInfo;
        String newHost = this.host.isNull() ? url.host.get() : this.host.get();
        Integer newPort = this.port.isNull() ? url.port.get() : this.port.get();
        String newPath = this.path.isNull() ? url.path.get() : this.path.get();
        String newRef = this.ref == null ? url.ref : this.ref;
        return new ForestURL(newSchema, newUserInfo, newHost, newPort, newPath, newRef);
    }



    /**
     * 设置基地址URL
     *
     * @param baseURL 基地址URL
     * @return {@link ForestURL}对象实例
     */
    public ForestURL setBaseURL(ForestURL baseURL) {
        String baseSchema = "http";
        String baseUserInfo = null;
        String baseHost = "localhost";
        int basePort = -1;
        String basePath = null;
        if (baseURL != null) {
            if (baseURL.scheme != null) {
                baseSchema = baseURL.scheme;
            }
            if (baseURL.userInfo != null) {
                baseUserInfo = baseURL.userInfo;
            }
            if (baseURL.host.get() != null) {
                baseHost = baseURL.host.get();
            }
            if (URLUtils.isNotNonePort(baseURL.port.get())) {
                basePort = baseURL.port.get();
            }
            if (baseURL.path.get() != null) {
                basePath = baseURL.path.get();
            }
        }
        boolean needBasePath = false;
        if (this.scheme == null) {
            setScheme(baseSchema);
            needBasePath = true;
        }
        if (this.userInfo == null) {
            this.userInfo = baseUserInfo;
        }
        if (this.host.isNull()) {
            this.host.set(baseHost);
            needBasePath = true;
        }

        if (URLUtils.isNonePort(this.port.get())) {
            this.port.set(basePort);
        }
        if (StringUtils.isNotBlank(this.path.get())) {
            if (this.path.get().charAt(0) != '/') {
                this.path.set('/' + this.path.get());
            }
        }
        if (needBasePath && StringUtils.isNotBlank(basePath)) {
            if (basePath.charAt(basePath.length() - 1) == '/') {
                basePath = basePath.substring(0, basePath.length() - 1);
            }
            if (StringUtils.isEmpty(this.path.get())) {
                this.path.set(basePath);
            } else {
                this.path.set(basePath + this.path.get());
            }
        }
        needRegenerateUrl();
        return this;
    }

    public ForestURL mergeAddress() {
        if (address != null) {
            String originHost = host.get();
            if (StringUtils.isEmpty(scheme)) {
                scheme = address.getScheme();
                refreshSSL();
            }
            if (StringUtils.isEmpty(host.get())) {
                host.set(address.getHost());
            }
            if (URLUtils.isNonePort(port.get()) && StringUtils.isEmpty(originHost)) {
                port.set(address.getPort());
            }
            if (StringUtils.isEmpty(userInfo)) {
                userInfo = address.getUserInfo();
            }
            if (StringUtils.isEmpty(basePath.get())) {
                setBasePath(address.getBasePath(), false);
            }
            needRegenerateUrl();
        }
        return this;
    }


    public ForestURL checkAndComplete() {
        String oldUrl = getGeneratedUrl();
        if (StringUtils.isEmpty(scheme)) {
            setScheme(ssl ? "https" : "http");
        }
        if (StringUtils.isEmpty(host.get())) {
            setHost("localhost");
            if (URLUtils.isNonePort(port.get())) {
                log.warn("[Forest] Invalid url '" + oldUrl + "'. But an valid url must start width 'http://' or 'https://'. Convert this url to '" + toURLString() + "' automatically!");
            } else {
                log.warn("[Forest] Invalid url '" + oldUrl + "'. Host is empty. Convert this url to '" + toURLString() + "' automatically!");
            }
        }
        return this;
    }


    public ForestURL bindRequest(ForestRequest<?> request) {
        this.host.bindRequest(request);
        this.port.bindRequest(request);
        this.basePath.bindRequest(request);
        this.path.bindRequest(request);
        needRegenerateUrl();
        return this;
    }

    @Override
    protected ForestURL clone() {
        final ForestURL newUrl = new ForestURL(this.scheme, this.userInfo, this.host.get(), this.port.get(), this.path.get(), this.ref);
        newUrl.basePath = this.basePath;
        newUrl.originalUrl = this.originalUrl;
        newUrl.address = this.address;
        return newUrl;
    }
}
