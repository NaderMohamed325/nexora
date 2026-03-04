package com.neo.nexora.service.user.lookUp;

import com.google.common.hash.BloomFilter;

public interface UserLookUpService {

    void addUser(String username);

    boolean mightContainUser(String username);

    boolean shouldCheckDatabase(String username);
}
