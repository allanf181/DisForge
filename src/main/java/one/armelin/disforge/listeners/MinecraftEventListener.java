package one.armelin.disforge.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import one.armelin.disforge.DisForge;
import one.armelin.disforge.utils.MarkdownParser;
import one.armelin.disforge.utils.Utils;
import org.jetbrains.annotations.NotNull;


public class MinecraftEventListener {

    public static final MediaType JSON = MediaType.get("application/json");

    @SubscribeEvent
    public void onChatMessage(ServerChatEvent event) {
        if (!DisForge.stop) {
            Tuple<String, String> convertedPair = Utils.convertMentionsFromNames(event.getRawText());
            ServerPlayer playerEntity = event.getPlayer();
            if (DisForge.config.isWebhookEnabled) {
                String json = getWebhookJson(playerEntity, convertedPair);
                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder()
                        .url(DisForge.config.webhookURL)
                        .post(body)
                        .build();
                try {
                    DisForge.webhookClient.newCall(request).execute();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                DisForge.textChannel.sendMessage(DisForge.config.texts.playerMessage.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName())).replace("%playermessage%", convertedPair.getA())).queue();
            }
            if (DisForge.config.modifyChatMessages) {
                JsonObject newComponent = new JsonObject();
                newComponent.addProperty("text", MarkdownParser.parseMarkdown(convertedPair.getB()));
                Component finalText = Component.Serializer.fromJson(newComponent.toString());
                event.setMessage(finalText);
            }
        }
    }

    @NotNull
    private static String getWebhookJson(ServerPlayer playerEntity, Tuple<String, String> convertedPair) {
        JsonObject body = new JsonObject();
        body.addProperty("username", playerEntity.getScoreboardName());
        body.addProperty("avatar_url", "https://mc-heads.net/avatar/" + (DisForge.config.useUUIDInsteadNickname ? playerEntity.getUUID() : playerEntity.getScoreboardName()));
        JsonObject allowed_mentions = new JsonObject();
        JsonArray parse = new JsonArray();
        parse.add("users");
        parse.add("roles");
        allowed_mentions.add("parse", parse);
        body.add("allowed_mentions", allowed_mentions);
        body.addProperty("content", convertedPair.getA());
        return body.toString();
    };

    @SubscribeEvent
    public void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event){
        Advancement advancement = event.getAdvancement();
        Player playerEntity = event.getEntity();
        if(DisForge.config.announceAdvancements && advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceChat() && !DisForge.stop) {
            switch (advancement.getDisplay().getFrame()) {
                case GOAL -> DisForge.textChannel.sendMessage(DisForge.config.texts.advancementGoal.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName())).replace("%advancement%", MarkdownSanitizer.escape(advancement.getDisplay().getTitle().getString()))).queue();
                case TASK -> DisForge.textChannel.sendMessage(DisForge.config.texts.advancementTask.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName())).replace("%advancement%", MarkdownSanitizer.escape(advancement.getDisplay().getTitle().getString()))).queue();
                case CHALLENGE -> DisForge.textChannel.sendMessage(DisForge.config.texts.advancementChallenge.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName())).replace("%advancement%", MarkdownSanitizer.escape(advancement.getDisplay().getTitle().getString()))).queue();
            }
        }
    };

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer && DisForge.config.announceDeaths && !DisForge.stop){
            ServerPlayer playerEntity = (ServerPlayer) event.getEntity();
            DamageSource damageSource = event.getSource();
            DisForge.textChannel.sendMessage(DisForge.config.texts.deathMessage.replace("%deathmessage%",MarkdownSanitizer.escape(damageSource.getLocalizedDeathMessage(playerEntity).getString())).replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName()))).queue();

        }
    };

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(DisForge.config.announcePlayers && !DisForge.stop){
            Player playerEntity = event.getEntity();
            DisForge.textChannel.sendMessage(DisForge.config.texts.joinServer.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName()))).queue();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if(DisForge.config.announcePlayers && !DisForge.stop){
            Player playerEntity = event.getEntity();
            DisForge.textChannel.sendMessage(DisForge.config.texts.leftServer.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName()))).queue();
        }
    }
}