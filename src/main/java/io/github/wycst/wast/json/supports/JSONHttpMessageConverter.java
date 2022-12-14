//package io.github.wycst.wast.json.supports;
//
//import io.github.wycst.wast.json.JSON;
//import io.github.wycst.wast.json.options.ReadOption;
//import io.github.wycst.wast.json.options.WriteOption;
//import org.springframework.http.HttpInputMessage;
//import org.springframework.http.HttpOutputMessage;
//import org.springframework.http.MediaType;
//import org.springframework.http.converter.AbstractHttpMessageConverter;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.http.converter.HttpMessageNotWritableException;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * springboot application/json HttpMessageConverter
// *
// * <pre>
// *
// * @Configuration
// * public class AppStart implements WebMvcConfigurer {
// *
// *  @Bean
// *  public HttpMessageConverters jsonHttpMessageConverters() {
// *      JSONHttpMessageConverter jsonHttpMessageConverter = new JSONHttpMessageConverter();
// *      jsonHttpMessageConverter.setWriteOptions(WriteOption... writeOptions);
// *      jsonHttpMessageConverter.setReadOptions(ReadOption... readOptions);
// *      return new HttpMessageConverters(jsonHttpMessageConverter);
// *  }
// *
// * }
// * </pre>
// *
// * @Author: wangy
// * @Date: 2022/1/22 16:06
// * @Description:
// */
//public class JSONHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
//
//    private WriteOption[] writeOptions;
//    private ReadOption[] readOptions;
//
//    public JSONHttpMessageConverter() {
//        this.initOptions();
//    }
//
//    public JSONHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
//        super(defaultCharset, supportedMediaTypes);
//        this.initOptions();
//    }
//
//    public JSONHttpMessageConverter(MediaType... supportedMediaTypes) {
//        super(supportedMediaTypes);
//        this.initOptions();
//    }
//
//    private void initOptions() {
//        writeOptions = new WriteOption[0];
//        readOptions = new ReadOption[0];
//    }
//
//    @Override
//    protected boolean supports(Class<?> aClass) {
//        return true;
//    }
//
//    @Override
//    public List<MediaType> getSupportedMediaTypes() {
//        List<MediaType> supportedMediaTypes = super.getSupportedMediaTypes();
//        if (supportedMediaTypes != null && supportedMediaTypes.size() > 0) {
//            return supportedMediaTypes;
//        }
//        List<MediaType> mediaTypes = new ArrayList<MediaType>();
//        mediaTypes.add(MediaType.APPLICATION_JSON);
//        return mediaTypes;
//    }
//
//    public void setWriteOptions(WriteOption... writeOptions) {
//        this.writeOptions = writeOptions;
//    }
//
//    public void setReadOptions(ReadOption... readOptions) {
//        this.readOptions = readOptions;
//    }
//
//    /***
//     *
//     *
//     * @param aClass
//     * @param httpInputMessage
//     * @return
//     * @throws IOException
//     * @throws HttpMessageNotReadableException
//     */
//    @Override
//    protected Object readInternal(Class<?> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
//        InputStream is = httpInputMessage.getBody();
//        return JSON.read(is, aClass, readOptions);
//    }
//
//    @Override
//    protected void writeInternal(Object o, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
//        OutputStream os = httpOutputMessage.getBody();
//        JSON.writeJsonTo(o, os, writeOptions);
//    }
//}
