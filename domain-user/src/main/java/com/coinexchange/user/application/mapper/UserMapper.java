package com.coinexchange.user.application.mapper;

import com.coinexchange.user.application.dto.UserLookupResponse;
import com.coinexchange.user.domain.User;

public class UserMapper {

    public static UserLookupResponse toLookupResponse(User user) {
        return new UserLookupResponse(user.getId(), user.getEmail());
    }
}
