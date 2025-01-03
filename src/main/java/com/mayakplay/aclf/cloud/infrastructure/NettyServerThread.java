package com.mayakplay.aclf.cloud.infrastructure;

import com.mayakplay.aclf.cloud.stereotype.ClientNuggetReceiveCallback;
import com.mayakplay.aclf.cloud.stereotype.ClientRegistrationHandler;
import com.mayakplay.aclf.cloud.stereotype.GatewayClientInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author mayakplay
 * @version 0.0.1
 * @since 20.07.2019.
 */
final class NettyServerThread extends Thread {

    private final int port;
    private final NettyServerHandler nettyServerHandler;
    private final GatewayClientsContainer clientsContainer;

    NettyServerThread(int port, ClientNuggetReceiveCallback receiveCallback, ClientRegistrationHandler registrationHandler, Map<String, String> parameters) {
        this.port = port;

        this.clientsContainer = new GatewayClientsContainer(parameters, receiveCallback, registrationHandler);
        this.nettyServerHandler = new NettyServerHandler(clientsContainer);
    }

    @Override
    public void run() {
        final ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel socketChannel) {
                System.out.println("Connected: " + "[" + socketChannel.remoteAddress() + "]");
                socketChannel.pipeline().addLast("framer", new DelimiterBasedFrameDecoder(81920, Delimiters.lineDelimiter()));
                socketChannel.pipeline().addLast("decoder", new StringDecoder());
                socketChannel.pipeline().addLast("encoder", new StringEncoder());
                socketChannel.pipeline().addLast("disconnectLogger", new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        System.out.println("Disconnected: " + "[" + ctx.channel().remoteAddress() + "]");
                        super.channelInactive(ctx);
                    }
                });
                socketChannel.pipeline().addLast(nettyServerHandler);
            }
        };

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(channelInitializer)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            System.out.println("Server start listen at " + port);
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }


    void sendToClient(@NotNull GatewayClientInfo clientInfo, @NotNull String message, @NotNull Map<String, String> params) {
        nettyServerHandler.sendToClient(clientInfo, new NuggetWrapper(message, params));
    }

    void sendToAll(@NotNull String message, @NotNull Map<String, String> params) {
        nettyServerHandler.sendToAll(new NuggetWrapper(message, params));
    }

    @NotNull
    Map<String, GatewayClientInfo> getClients() {
        return clientsContainer.getClients();
    }

    @NotNull
    Map<String, GatewayClientInfo> getClientsByType(String type) {
        return clientsContainer.getClientsByType(type);
    }

    @Nullable
    GatewayClientInfo getClientById(String clientId) {
        return clientsContainer.getClientInfoById(clientId);
    }
}