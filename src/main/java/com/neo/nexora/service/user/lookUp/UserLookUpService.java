package com.neo.nexora.service.user.lookUp;

public interface UserLookUpService {

    void addUser(String username);

    boolean mightContainUser(String username);

    boolean shouldCheckDatabase(String username);
}
