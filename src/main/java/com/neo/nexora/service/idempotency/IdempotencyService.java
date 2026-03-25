package com.neo.nexora.service.idempotency;

public interface IdempotencyService {
    /*
    I think it is overkill to do it on posts or comments, mostly it's for a learning purpose
     */
    public void validate(String key, String userId);
}
