package com.dream11.redis.operation;


import com.dream11.redis.config.user.UpdateReplicaCountConfig;
import com.dream11.redis.service.RedisService;
import com.dream11.redis.service.StateService;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class UpdateReplicaCount implements Operation {
    @NonNull final RedisService redisService;
    @NonNull final StateService stateService;
    @NonNull final UpdateReplicaCountConfig updateReplicaCountConfig;

    @Override
    public void execute() {
        this.stateService.reconcileState();
        this.redisService.updateReplicaCount(updateReplicaCountConfig);
    }
}
