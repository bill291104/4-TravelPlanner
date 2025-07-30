package com.fastcampus.toyproject4_team4.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) throw new RuntimeException("application.properties 파일을 찾을 수 없습니다.");
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("설정 파일 로딩 실패", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
