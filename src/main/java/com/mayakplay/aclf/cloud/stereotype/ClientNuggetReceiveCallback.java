package com.mayakplay.aclf.cloud.stereotype;

/**
 * @author mayakplay
 * @version 0.0.1
 * @since 20.07.2019.
 */
@FunctionalInterface
public interface ClientNuggetReceiveCallback {

    void nuggetReceived(GatewayClientInfo clientInfo, Nugget nugget);

}