package com.neo.nexora.service.follow;

import org.springframework.security.core.userdetails.UserDetails;

public interface FollowService {


    void followUser(UserDetails userDetails, Long followeeId);


    void unfollowUser(UserDetails userDetails, Long followeeId);
}
