package com.mayakplay.aclf.cloud.infrastructure;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ReconnectInboundHandler extends ChannelInboundHandlerAdapter {
    private final NettyClientThread clientThread;

    public ReconnectInboundHandler(NettyClientThread clientThread) {
        this.clientThread = clientThread;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Disconnected from server, attempting to reconnect...");
        clientThread.createBootstrap(new Bootstrap(), ctx.channel().eventLoop());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Error: " + cause.getMessage());
        ctx.close();
    }
}
