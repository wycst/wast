package io.github.wycst.wast.clients.http.definition;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: wangy
 * @Date: 2020/7/2 16:38
 * @Description:
 */
public interface HttpClientResponse {

    public int status();

    public String reasonPhrase();

    public byte[] content();

    public int length();

    void setContentType(String contentType);

    public String getContentType();

    public <E> E getEntity(Class<E> entityCls);

    public <E> E getEntity(GenericParameterizedType<E> parameterizedType, ReadOption... readOptions);

    public <E> List<E> getEntityList(Class<E> entityCls);

    void setHeaders(Map<String, List<String>> responseHeaders);

    public String getHeader(String name);

    public List<String> getHeaders(String name);

    public Set<String> getHeaderNames();

    /**
     * 只有HttpClientConfig设置responseStream为true的场景会返回非空的流，否则返回null
     * 
     * @return
     * @see HttpClientConfig#responseStream(boolean)
     */
    InputStream inputStream();

    /**
     * 从请求头中读取实际的Content-Length并转化为long值；
     * (注意HttpURLConnection#getContentLength()返回的类型为int,对于超大文件流无法获取准确的值)；
     *
     * @return
     * @see HttpURLConnection#getContentLength()
     */
    long readContentLength();
}
