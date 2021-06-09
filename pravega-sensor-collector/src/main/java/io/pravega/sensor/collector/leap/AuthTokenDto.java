/**
 * Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.pravega.sensor.collector.leap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthTokenDto {
    public String userId;
    public String displayName;
    public String token;
    public List<String> roles;
    public Integer expires_in;

    public AuthTokenDto(String userId, String displayName, String token, List<String> roles,Integer expires_in) {
        this.userId = userId;
        this.displayName = displayName;
        this.token=token;
        this.roles=roles;
        this.expires_in=expires_in;
    }

    @Override
    public String toString() {
        return "AuthTokenDto{" +
                "userId=" + userId +
                ", displayName=" + displayName +
                ", token=" + token +
                ", roles=" + roles +
                ", expires_in=" + expires_in +
                '}';
    }
}
