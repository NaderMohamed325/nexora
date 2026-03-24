package com.neo.nexora.service.follow;


import com.neo.nexora.entity.User;
import com.neo.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {
    private final UserRepository userRepository;


    @Override
    public void followUser(UserDetails userDetails, Long followeeId) {
        User follower = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + followeeId));
        Set<User> following = follower.getFollowing();
        if (following.contains(followee)) {
            log.info("User '{}' is already following user id={}", follower.getUsername(), followeeId);
        } else {
            following.add(followee);
            userRepository.save(follower);
            log.info("User '{}' started following user id={}", follower.getUsername(), followeeId);
        }
    }


    @Override
    public void unfollowUser(UserDetails userDetails, Long followeeId) {
        User follower = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + followeeId));
        Set<User> following = follower.getFollowing();
        if (!following.contains(followee)) {
            log.info("User '{}' is not following user id={}", follower.getUsername(), followeeId);
        } else {
            following.remove(followee);
            userRepository.save(follower);
            log.info("User '{}' unfollowed user id={}", follower.getUsername(), followeeId);
        }
    }
}
