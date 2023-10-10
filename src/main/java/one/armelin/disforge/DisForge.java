package one.armelin.disforge;

import com.mojang.logging.LogUtils;
import kong.unirest.Unirest;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.commands.Commands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import one.armelin.disforge.commands.ShrugCommand;
import one.armelin.disforge.listeners.DiscordEventListener;
import one.armelin.disforge.listeners.MinecraftEventListener;
import org.slf4j.Logger;

import java.util.Collections;

@Mod(DisForge.MODID)
public class DisForge {

    public static final String MODID = "disforge";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static Configuration config;
    public static JDA jda;
    public static GuildMessageChannel textChannel;
    public static boolean stop = false;


    public DisForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::serverSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void serverSetup(final FMLDedicatedServerSetupEvent event) {
        AutoConfig.register(Configuration.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(Configuration.class).getConfig();
        LOGGER.info("teste");
        LOGGER.info(JDAInfo.VERSION);

        try {
            JDABuilder jdaBuilder = JDABuilder.createDefault(config.botToken).setHttpClient(new OkHttpClient.Builder()
                            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                            .build())
                    .addEventListeners(new DiscordEventListener())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT);
            if(config.membersIntents){
                jdaBuilder.enableIntents(GatewayIntent.GUILD_MEMBERS).setMemberCachePolicy(MemberCachePolicy.ALL);
            }
            DisForge.jda = jdaBuilder.build();
            DisForge.jda.awaitReady();
            DisForge.textChannel = (GuildMessageChannel) DisForge.jda.getGuildChannelById(config.channelId);
        } catch (InvalidTokenException ex) {
            jda = null;
            DisForge.LOGGER.error("Unable to login!", ex);
        } catch (InterruptedException ex) {
            jda = null;
            DisForge.LOGGER.error("Exception", ex);
        }
        if(jda != null) {
            if (!config.botGameStatus.isEmpty())
                jda.getPresence().setActivity(Activity.playing(config.botGameStatus));
            MinecraftForge.EVENT_BUS.register(new MinecraftEventListener());
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if(jda != null){
            textChannel.sendMessage(DisForge.config.texts.serverStarted).queue();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if(jda != null){
            stop = true;
            textChannel.sendMessage(DisForge.config.texts.serverStopped).queue();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Unirest.shutDown();
            DisForge.jda.shutdown();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if(event.getCommandSelection() == Commands.CommandSelection.DEDICATED){
            ShrugCommand.register(event.getDispatcher());
        }
    }
}
