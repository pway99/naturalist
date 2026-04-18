package com.naturalist.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Supplier;

public class TestDataHelper {
    static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public static String readFileToString(String path) {
        ClassLoader classLoader = TestDataHelper.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(path)) {
            if (is == null) {
                throw new FileNotFoundException(path);
            }
            return new String(is.readAllBytes());
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    public static <T> List<T> readObjectsFromString(Supplier<String> data, Class<T> clazz) {
        try {
            return mapper.readerForListOf(clazz).readValue(data.get());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
