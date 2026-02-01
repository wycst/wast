package io.github.wycst.wast.json;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

/**
 * @Date 2024/9/29 13:46
 * @Created by wangyc
 */
final class JSONTypeExtensionSer {

    static void initExtens() {
        JSONTypeSerializer.putTypeSerializer(JSONTypeSerializer.GLOBAL_SERIALIZERS, new JSONTypeSerializer() {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                writer.writeUUID((UUID) value);
            }
        }, UUID.class);
        JSONTypeSerializer.putTypeSerializer(JSONTypeSerializer.GLOBAL_SERIALIZERS, new JSONTypeSerializer() {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                JSONNode jsonNode = (JSONNode) value;
                JSONStore.INSTANCE.ANY_SER.serialize(jsonNode.any(), writer, jsonConfig, indent);
            }
        }, JSONNode.class, JSONNode.B.class, JSONNode.C.class, JSONNode.D.class);
        JSONTypeSerializer.putTypeSerializer(JSONTypeSerializer.GLOBAL_SERIALIZERS, JSONTypeSerializer.TO_STRING, URL.class, URI.class);
    }
}
