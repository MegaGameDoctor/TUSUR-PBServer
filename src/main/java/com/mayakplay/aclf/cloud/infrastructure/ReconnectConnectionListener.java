package com.mayakplay.aclf.cloud.infrastructure;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

import java.util.concurrent.TimeUnit;

/**
 * @author mayakplay
 * @since 20.08.2019.
 */
class ReconnectConnectionListener implements ChannelFutureListener {

    private static final int MAX_ATTEMPTS = 10;
    private final NettyClientThread clientThread;
    private int attemptCount = 0;

    ReconnectConnectionListener(NettyClientThread clientThread) {
        this.clientThread = clientThread;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (!channelFuture.isSuccess()) {
            System.out.println("Reconnect attempt " + attemptCount);

            if (attemptCount >= MAX_ATTEMPTS) {
                System.err.println("Max reconnect attempts reached. Giving up.");
                return;
            }

            attemptCount++;
            final EventLoop loop = channelFuture.channel().eventLoop();
            long delay = Math.min(2L * attemptCount, 30L);
            loop.schedule(() -> {
                try {
                    clientThread.createBootstrap(new Bootstrap(), loop);
                } catch (Exception e) {
                    System.err.println("Failed to reconnect: " + e.getMessage());
                    e.printStackTrace();
                }
            }, delay, TimeUnit.SECONDS);
        } else {
            attemptCount = 0;
            System.out.println("Successfully connected to server!");
        }
    }
}
