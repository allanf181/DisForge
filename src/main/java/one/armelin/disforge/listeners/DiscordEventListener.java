package one.armelin.disforge.listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import one.armelin.disforge.DisForge;
import one.armelin.disforge.utils.DiscordCommandOutput;
import one.armelin.disforge.utils.MarkdownParser;
import one.armelin.disforge.utils.Utils;
import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.ParseResults;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DiscordEventListener extends ListenerAdapter {

    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        MinecraftServer server = getServer();
        if(e.getAuthor() != e.getJDA().getSelfUser() && !e.getAuthor().isBot() && e.getChannel().getId().equals(DisForge.config.channelId) && server != null) {
            if(e.getMessage().getContentRaw().startsWith("!console") && Arrays.asList(DisForge.config.adminsIds).contains(e.getAuthor().getId())) {
                String command = e.getMessage().getContentRaw().replace("!console ", "");
                CommandSourceStack source = getDiscordCommandSource();
                ParseResults<CommandSourceStack> results = server.getCommands().getDispatcher().parse(command, source);
                server.getCommands().performCommand(results, command);

            } else if(e.getMessage().getContentRaw().startsWith("!online")) {
                List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();
                StringBuilder playerList = new StringBuilder("```\n=============== Online Players (" + onlinePlayers.size() + ") ===============\n");
                for (ServerPlayer player : onlinePlayers) {
                    playerList.append("\n").append(player.getScoreboardName());
                }
                playerList.append("```");
                e.getChannel().sendMessage(playerList.toString()).queue();

            } else if (e.getMessage().getContentRaw().startsWith("!tps")) {
                StringBuilder tpss = new StringBuilder("Server TPS: ");
                double serverTickTime = Utils.average(server.tickTimes) * 1.0E-6D;
                tpss.append(Math.min(1000.0 / serverTickTime, 20));
                e.getChannel().sendMessage(tpss.toString()).queue();

            } else if(e.getMessage().getContentRaw().startsWith("!help")){
                String help = """
                        ```
                        =============== Commands ===============

                        !online: list server online players
                        !tps: shows loaded dimensions tps´s
                        !console <command>: executes commands in the server console (admins only)
                        ```""";
                e.getChannel().sendMessage(help).queue();
            } else {
                MutableComponent discord = Component.literal(applyPlaceholders(DisForge.config.texts.coloredText, e));
                discord.setStyle(discord.getStyle().withColor(TextColor.fromRgb(Objects.requireNonNull(e.getMember()).getColorRaw())));
                MutableComponent msg = Component.literal(applyPlaceholders(DisForge.config.texts.colorlessText, e));
                msg.setStyle(msg.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.WHITE)));
                if (e.getMessage().getReferencedMessage() != null){
                    String user;
                    if(e.getMessage().getReferencedMessage().getMember() != null){
                        user = e.getMessage().getReferencedMessage().getMember().getEffectiveName();
                    } else {
                        user = e.getMessage().getReferencedMessage().getAuthor().getEffectiveName();
                    }
                    MutableComponent reply = Component.literal(DisForge.config.texts.replyText
                            .replace("%discordname%", Utils.sanitize(user, true)));
                    reply.setStyle(reply.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY)));
                    server.getPlayerList().getPlayers().forEach(serverPlayerEntity ->
                            serverPlayerEntity.displayClientMessage(Component.literal("")
                                    .append(discord)
                                    .append(reply)
                                    .append(msg),false));
                }
                else {
                    server.getPlayerList().getPlayers().forEach(serverPlayerEntity ->
                            serverPlayerEntity.displayClientMessage(Component.literal("")
                                    .append(discord)
                                    .append(msg),false));
                }
            }
        }

    }

    public CommandSourceStack getDiscordCommandSource(){
        ServerLevel serverWorld = getServer().overworld();
        return new CommandSourceStack(new DiscordCommandOutput(), Vec3.atLowerCornerOf(serverWorld.getSharedSpawnPos()), Vec2.ZERO, serverWorld, 4, "Discord", Component.literal("Discord"), getServer(), null);
    }

    private MinecraftServer getServer(){
        return ServerLifecycleHooks.getCurrentServer();
    }

    private static String applyPlaceholders(String text, MessageReceivedEvent e) {
        return text.replace("%discordname%", Utils.sanitize(Objects.requireNonNull(e.getMember()).getEffectiveName(), true))
                .replace("%message%", MarkdownParser.parseMarkdown(Utils.sanitize(e.getMessage().getContentDisplay(), false)
                        + ((!e.getMessage().getAttachments().isEmpty()) ? " <att>" : "")
                        + ((!e.getMessage().getEmbeds().isEmpty()) ? " <embed>" : "")));
    }
}