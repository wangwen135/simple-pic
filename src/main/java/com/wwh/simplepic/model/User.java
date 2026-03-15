package com.wwh.simplepic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User model
 * 用户模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String username;
    private String password;
    private Role role;
    private String[] storageSpaces;
    private String currentStorageSpace;

    public User(String username, String password, Role role, String[] storageSpaces) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.storageSpaces = storageSpaces;
        this.currentStorageSpace = storageSpaces.length > 0 ? storageSpaces[0] : null;
    }
}