package one.armelin.disforge.listeners;

import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
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
import one.armelin.disforge.DisForge;
import one.armelin.disforge.utils.MarkdownParser;
import one.armelin.disforge.utils.Utils;

public class MinecraftEventListener {

    @SubscribeEvent
    public void onChatMessage(ServerChatEvent event) {
        if (!DisForge.stop) {
            Tuple<String, String> convertedPair = Utils.convertMentionsFromNames(event.getRawText());
            ServerPlayer playerEntity = event.getPlayer();
            if (DisForge.config.isWebhookEnabled) {
                JSONObject body = new JSONObject();
                body.put("username", playerEntity.getScoreboardName());
                body.put("avatar_url", "https://mc-heads.net/avatar/" + (DisForge.config.useUUIDInsteadNickname ? playerEntity.getUUID() : playerEntity.getScoreboardName()));
                JSONObject allowed_mentions = new JSONObject();
                allowed_mentions.put("parse", new String[]{"users", "roles"});
                body.put("allowed_mentions", allowed_mentions);
                body.put("content", convertedPair.getA());
                try {
                    Unirest.post(DisForge.config.webhookURL).header("Content-Type", "application/json").body(body).asJsonAsync();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                DisForge.textChannel.sendMessage(DisForge.config.texts.playerMessage.replace("%playername%", MarkdownSanitizer.escape(playerEntity.getScoreboardName())).replace("%playermessage%", convertedPair.getA())).queue();
            }
            if (DisForge.config.modifyChatMessages) {
                JSONObject newComponent = new JSONObject();
                newComponent.put("text", MarkdownParser.parseMarkdown(convertedPair.getB()));
                Component finalText = Component.Serializer.fromJson(newComponent.toString());
                event.setMessage(finalText);
            }
        }
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