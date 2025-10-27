package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/7/2 19:12
 * @Description:
 */
public class HttpClientResponseImpl extends AbstractHttpClientResponse {

    public HttpClientResponseImpl(int code, String reasonPhrase, InputStream is, int contentLength, String contentType, Map<String, List<String>> headers) {
        super(code, reasonPhrase, is, contentLength, contentType, headers);
    }

    public HttpClientResponseImpl(int code, String reasonPhrase, byte[] conent, int contentLength, String contentType, Map<String, List<String>> headers) {
        super(code, reasonPhrase, conent, contentLength, contentType, headers);
    }

    public <E> E getEntity(Class<E> entityCls) {
        byte[] content = content();
        if (content == null)
            return null;
        if (entityCls == HttpClientResponse.class) {
            return (E) this;
        }
        if (byte[].class.isAssignableFrom(entityCls)) {
            return (E) content;
        }
        if (InputStream.class.isAssignableFrom(entityCls)) {
            return (E) new ByteArrayInputStream(content);
        }
        String text = new String(content);
        if (entityCls == String.class) {
            return (E) text;
        }
        text = text.trim();
        if (text.startsWith("{") && text.endsWith("}")) {
            return JSON.parseObject(text, entityCls);
        }

        if (Collection.class.isAssignableFrom(entityCls) && text.startsWith("[") && text.endsWith("]")) {
            Class<? extends Collection> collCls = (Class<? extends Collection>) entityCls;
            return (E) JSON.parseCollection(text, collCls);
        }

        return null;
    }

    @Override
    public <E> E getEntity(GenericParameterizedType<E> parameterizedType, ReadOption... readOptions) {
        byte[] content = content();
        if (content == null)
            return null;
        return JSON.parse(content, parameterizedType, readOptions);
    }

    public <E> List<E> getEntityList(Class<E> entityCls) {
        byte[] content = content();
        if (content == null)
            return null;
        String text = new String(content).trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            return JSON.parseArray(text, entityCls);
        }
        return null;
    }


    @Override
    public String toString() {
        return "Response : " + status() + " " + reasonPhrase();
    }


}
