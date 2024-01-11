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

public class AuthCredentialsDto {
    public final String userName;
    public final  String password;

    public AuthCredentialsDto(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public String toString() {
        return "AuthCredentialsDto{"
                + "userName=" + userName
                + ", password=" + password
                + '}';
    }
}
