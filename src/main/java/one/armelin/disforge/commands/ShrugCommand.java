package one.armelin.disforge.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;

import java.util.Objects;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ShrugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("shrug").then(argument("message", MessageArgument.message()).executes(context -> {
            CommandSourceStack serverCommandSource = context.getSource();
            PlayerList playerManager = serverCommandSource.getServer().getPlayerList();
            MessageArgument.resolveChatMessage(context, "message", signedMessage -> {
                ServerChatEvent event = new ServerChatEvent(Objects.requireNonNull(serverCommandSource.getPlayer()), signedMessage.decoratedContent().getString() + " ¯\\_(ツ)_/¯", Component.nullToEmpty(signedMessage.decoratedContent().getString() + " ¯\\_(ツ)_/¯"));
                Optional<Component> eventResult = MinecraftForge.EVENT_BUS.post(event) ? Optional.empty() : Optional.ofNullable(event.getMessage());
                playerManager.broadcastChatMessage(signedMessage.withUnsignedContent(eventResult.orElse(Component.nullToEmpty(signedMessage.decoratedContent().getString() + " ¯\\_(ツ)_/¯"))) , serverCommandSource, ChatType.bind(ChatType.CHAT, serverCommandSource));
            });
            return 1;
        }
        )));
        dispatcher.register(literal("shrug").executes(context -> {
            CommandSourceStack serverCommandSource = context.getSource();
            PlayerList playerManager = serverCommandSource.getServer().getPlayerList();
            String raw = "¯\\_(ツ)_/¯";
            ServerChatEvent event = new ServerChatEvent(Objects.requireNonNull(serverCommandSource.getPlayer()), raw, Component.nullToEmpty(raw));
            Optional<Component> eventResult = MinecraftForge.EVENT_BUS.post(event) ? Optional.empty() : Optional.ofNullable(event.getMessage());
            playerManager.broadcastChatMessage(PlayerChatMessage.unsigned(serverCommandSource.getPlayer().getUUID(),eventResult.orElse(Component.nullToEmpty(raw)).getString()), serverCommandSource, ChatType.bind(ChatType.CHAT, serverCommandSource));
            return 1;
        }
        ));
    }
}
