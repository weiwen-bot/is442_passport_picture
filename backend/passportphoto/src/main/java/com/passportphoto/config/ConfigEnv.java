package com.passportphoto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigEnv {
    @Value("${opencv.dll.path}")
    private String ddlpath;

    public String getDdlpath() {
        return ddlpath;
    }
}

