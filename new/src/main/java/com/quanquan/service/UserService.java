package com.quanquan.service;

import com.quanquan.dto.User;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public interface UserService {
    void removeUser(int userId);

    List<User> getAllUsers();

    User getUser(int userId);
}
