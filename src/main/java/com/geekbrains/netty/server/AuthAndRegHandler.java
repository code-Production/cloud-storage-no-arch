package com.geekbrains.netty.server;

import com.geekbrains.netty.common.AbstractCommand;
import com.geekbrains.netty.common.Commands;
import com.geekbrains.netty.common.DatabaseCommand;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Map;

@Slf4j
public class AuthAndRegHandler extends SimpleChannelInboundHandler<AbstractCommand> {


    private final UserService userService;
    private final Map<Channel, String> clients;


    public AuthAndRegHandler(UserService userService, Map<Channel, String> clients) {
        this.userService = userService;
        this.clients = clients;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractCommand msg) throws Exception {
        switch(msg.getCommand()) {
            case AUTH_REQUEST -> {
                DatabaseCommand command = (DatabaseCommand) msg;
                command.setCommand(Commands.AUTH_RESPONSE);
                String login = command.getLogin();
                String password = command.getPassword();
                log.debug("Got new authentication command from client '{}'.", login);
                boolean isSuccess = false;
                try {
                    isSuccess = userService.authenticate(login, password);
                    command.setSuccess(isSuccess);
                    log.info("Client '{}' successfully logged in.", login);
                } catch (SQLException e) {
                    String response = String.format("Authentication process caused SQL exception, %s.", e);
                    command.setResponse(response);
                    log.debug(response);
                }
                ctx.writeAndFlush(command);
                if (isSuccess) {
                    NettyServer.addClient(ctx.channel(), login);
                    NettyServer.commandReadyPipeline(ctx.pipeline());
                }
//                System.out.println("AUTH_REQUEST");
            }
            case REGISTER_REQUEST -> {
                DatabaseCommand command = (DatabaseCommand) msg;
                command.setCommand(Commands.REGISTER_RESPONSE);
                String login = command.getLogin();
                String password = command.getPassword();
                String username = command.getUsername();
                log.debug("Got new registration command from client '{}'.", login);
                boolean isSuccess = false;
                try {
                    isSuccess = userService.register(username, login, password);
                    command.setSuccess(isSuccess);
                    log.info("Client '{}' successfully registered.", login);
                } catch (SQLException e) {
                    String response = String.format("Registration process caused SQL exception, %s.", e);
                    command.setResponse(response);
                    log.debug(response);
                }
                ctx.writeAndFlush(command);
//                System.out.println("REGISTER_REQUEST");
            }
        }
    }
}
