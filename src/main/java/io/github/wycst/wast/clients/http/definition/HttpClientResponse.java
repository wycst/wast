package io.github.wycst.wast.clients.http.definition;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.options.ReadOption;

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

    public <E> E getEntity(GenericParameterizedType<E> parameterizedType, ReadOption...readOptions);

    public <E> List<E> getEntityList(Class<E> entityCls);

    void setHeaders(Map<String, List<String>> responseHeaders);

    public String getHeader(String name);

    public List<String> getHeaders(String name);

    public Set<String> getHeaderNames();

}
