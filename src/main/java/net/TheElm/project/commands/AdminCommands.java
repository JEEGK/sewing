/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.TheElm.project.CoreMod;
import net.TheElm.project.ServerCore;
import net.TheElm.project.config.ConfigOption;
import net.TheElm.project.config.SewConfig;
import net.TheElm.project.enums.OpLevels;
import net.TheElm.project.enums.Permissions;
import net.TheElm.project.exceptions.ExceptionTranslatableServerSide;
import net.TheElm.project.utilities.CommandUtils;
import net.TheElm.project.utilities.TranslatableServerSide;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Stream;

public final class AdminCommands {
    
    public static final @NotNull String FLIGHT = "Fly";
    public static final @NotNull String GOD = "God";
    public static final @NotNull String HEAL = "Heal";
    public static final @NotNull String REPAIR = "Repair";
    
    private static final ExceptionTranslatableServerSide PLAYERS_NOT_FOUND_EXCEPTION = TranslatableServerSide.exception("player.none_found");
    
    private AdminCommands() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the FLY command
        ServerCore.register(dispatcher, AdminCommands.FLIGHT, (builder) -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_FLY))
            .then( CommandManager.argument( "target", EntityArgumentType.players())
                .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_FLY.onOther()))
                .executes(AdminCommands::targetFlying)
            )
            .executes(AdminCommands::selfFlying)
        );
        
        // Register the GOD command
        ServerCore.register(dispatcher, AdminCommands.GOD, (builder) -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GODMODE))
            .then( CommandManager.argument( "target", EntityArgumentType.players())
                .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_GODMODE.onOther()))
                .executes(AdminCommands::targetGod)
            )
            .executes(AdminCommands::selfGod)
        );
        
        // Register the HEAL command
        ServerCore.register(dispatcher, AdminCommands.HEAL, (builder) -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_HEAL))
            .then( CommandManager.argument( "target", EntityArgumentType.players())
                .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_HEAL.onOther()))
                .executes(AdminCommands::targetHeal)
            )
            .executes(AdminCommands::selfHeal)
        );
        
        // Register the HEAL command
        ServerCore.register(dispatcher, AdminCommands.REPAIR, (builder) -> builder
            .requires(CommandUtils.either(OpLevels.CHEATING, Permissions.PLAYER_REPAIR))
            .executes(AdminCommands::selfRepair)
        );
        
        // Create DEBUG commands
        if (CoreMod.isDebugging()) {
            ServerCore.register(dispatcher, "Dragon Players", (builder) -> builder
                .then(CommandManager.argument("count", IntegerArgumentType.integer( 0 ))
                    .executes((context) -> {
                        SewConfig.set(SewConfig.DRAGON_PLAYERS, ConfigOption.convertToJSON(
                            IntegerArgumentType.getInteger( context, "count" )
                        ));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );
        }
    }
    
    private static int selfFlying(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        AdminCommands.toggleFlying(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int targetFlying(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0)
            throw PLAYERS_NOT_FOUND_EXCEPTION.create( source );
        return AdminCommands.toggleFlying(source, players.stream());
    }
    private static int toggleFlying(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> TranslatableServerSide.send(source, "player.abilities.flying_other." + (AdminCommands.toggleFlying(player) ? "enabled" : "disabled"), player.getDisplayName()));
        return Command.SINGLE_SUCCESS;
    }
    private static boolean toggleFlying(@NotNull ServerPlayerEntity player) {
        // Toggle flying for the player
        player.abilities.allowFlying = !player.abilities.allowFlying;
        player.setNoGravity(false);
        
        // Tell the player
        TranslatableServerSide.send(player, "player.abilities.flying_self." + (player.abilities.allowFlying ? "enabled" : "disabled"));
        
        // If flying was turned off, stop the playing mid-flight
        if (!player.abilities.allowFlying)
            player.abilities.flying = false;
        
        player.sendAbilitiesUpdate();
        return player.abilities.allowFlying;
    }
    
    private static int selfGod(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        AdminCommands.toggleGod(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int targetGod(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0 )
            throw PLAYERS_NOT_FOUND_EXCEPTION.create( source );
        return AdminCommands.toggleGod(source, players.stream());
    }
    private static int toggleGod(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> TranslatableServerSide.send(source, "player.abilities.godmode_other." + (AdminCommands.toggleGod(player) ? "enabled" : "disabled"), player.getDisplayName()));
        return Command.SINGLE_SUCCESS;
    }
    private static boolean toggleGod(ServerPlayerEntity player) {
        // Toggle god mode for the player
        player.abilities.invulnerable = !player.abilities.invulnerable;
        player.sendAbilitiesUpdate();
    
        // Tell the player
        TranslatableServerSide.send(player, "player.abilities.godmode_self." + (player.abilities.invulnerable ? "enabled" : "disabled"));
        return player.abilities.invulnerable;
    }
    
    private static int selfHeal(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        AdminCommands.healPlayer(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int targetHeal(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
        if (players.size() <= 0 )
            throw PLAYERS_NOT_FOUND_EXCEPTION.create( source );
        return AdminCommands.healPlayer(source, players.stream());
    }
    private static int healPlayer(@NotNull ServerCommandSource source, @NotNull Stream<ServerPlayerEntity> players) {
        players.forEach(player -> TranslatableServerSide.send(source, (AdminCommands.healPlayer(player)? "player.abilities.healed_other" : "player.abilities.healed_dead"), player.getDisplayName()));
        return Command.SINGLE_SUCCESS;
    }
    private static boolean healPlayer(@NotNull ServerPlayerEntity player) {
        boolean alive;
        if (alive = player.isAlive()) {
            // Heal the player
            player.setHealth(player.getMaxHealth());
            
            // Tell the player
            TranslatableServerSide.send(player, "player.abilities.healed_self");
        }
        return alive;
    }
    
    private static int selfRepair(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        ItemStack stack = player.getMainHandStack();
        if (stack.isDamageable()) {
            stack.setDamage(0);
            
            return Command.SINGLE_SUCCESS;
        }
        return -1;
    }
    
}
