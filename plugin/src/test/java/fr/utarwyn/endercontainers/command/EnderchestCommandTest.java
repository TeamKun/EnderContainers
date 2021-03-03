package fr.utarwyn.endercontainers.command;

import fr.utarwyn.endercontainers.TestHelper;
import fr.utarwyn.endercontainers.configuration.wrapper.YamlFileLoadException;
import fr.utarwyn.endercontainers.enderchest.EnderChestManager;
import fr.utarwyn.endercontainers.enderchest.context.PlayerContext;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnderchestCommandTest extends CommandTestHelper<EnderchestCommand> {

    @Mock
    private EnderChestManager manager;

    @Mock
    private PlayerContext context;

    private Player player;

    @BeforeClass
    public static void setUpClass() throws ReflectiveOperationException, YamlFileLoadException,
            InvalidConfigurationException, IOException {
        TestHelper.setUpFiles();
    }

    @Before
    public void setUp() throws ReflectiveOperationException {
        TestHelper.registerManagers(this.manager);
        this.player = TestHelper.getPlayer();
        this.command = new EnderchestCommand();
        this.permission = "endercontainers.cmd.enderchests";

        doAnswer(answer -> {
            ((Consumer<PlayerContext>) answer.getArgument(1)).accept(this.context);
            return null;
        }).when(this.manager).loadPlayerContext(any(), any());
    }

    @Test
    public void create() {
        assertThat(this.command.getName()).isEqualTo("enderchest");
        assertThat(this.command.getAliases()).containsExactly("ec", "endchest");
    }

    @Test
    public void disableInConsole() {
        ConsoleCommandSender sender = mock(ConsoleCommandSender.class);
        this.run(sender);
        verify(sender).sendMessage(contains("player"));
    }

    @Test
    public void disabledWorld() {
        when(this.player.getWorld().getName()).thenReturn("disabled");
        this.run(this.player);
        verify(this.player).sendMessage(contains("disabled"));
        verify(this.manager, never()).loadPlayerContext(any(), any());
    }

    @Test
    public void openMainChest() {
        this.givePermission(this.player);
        this.run(this.player);
        verify(this.context).openHubMenuFor(this.player);
    }

    @Test
    public void openSpecificChest() {
        // With global permission
        this.givePermission(this.player);
        this.run(this.player, "10");
        verify(this.context).openEnderchestFor(this.player, 9);

        // With chest specific permission
        this.setPermissionState(this.player, false);
        when(this.player.hasPermission("endercontainers.cmd.enderchest.4")).thenReturn(true);
        this.run(this.player, "5");
        verify(this.context).openEnderchestFor(this.player, 4);
    }

    @Test
    public void noPermission() {
        // No permission for all chests
        this.run(this.player);

        // No permission for specific chest
        this.run(this.player, "5");

        verify(this.player).hasPermission("endercontainers.cmd.enderchest.4");
        this.verifyNoPerm(this.player, 2);
    }

    @Test
    public void errorEnderchestNumber() {
        this.givePermission(this.player);
        this.run(this.player, "-20");
        this.run(this.player, "0");
        this.run(this.player, "500");
        this.verifyNoPerm(this.player, 3);

        this.run(this.player, "ezaeza");
        verify(this.player).sendMessage(contains("not valid"));
    }

    @Test
    public void errorEnderchestNotAccessible() {
        this.givePermission(this.player);
        when(this.context.openEnderchestFor(this.player, 4)).thenReturn(false);
        this.run(this.player, "5");
        verify(this.player).sendMessage(contains("open"));
    }

}
