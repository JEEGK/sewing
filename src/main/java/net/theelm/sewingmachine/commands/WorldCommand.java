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

package net.theelm.sewingmachine.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.theelm.sewingmachine.ServerCore;
import net.theelm.sewingmachine.config.SewConfig;
import net.theelm.sewingmachine.enums.OpLevels;
import net.theelm.sewingmachine.interfaces.CommandPredicate;
import net.theelm.sewingmachine.utilities.WarpUtils;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public final class WorldCommand {
    
    private WorldCommand() {}
    
    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> gamerules = CommandManager.literal("gamerule");
        GameRules.accept(new GameRules.Visitor() {
            public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                gamerules.then(CommandManager.literal(key.getName())
                    .executes((context) -> WorldCommand.queryGameRule(context, key)
                ).then(type.argument("value")
                    .executes((context) -> WorldCommand.setGameRule(context, key)))
                );
            }
        });
        
        ServerCore.register(dispatcher, "world", builder -> builder
            .requires(CommandPredicate.opLevel(OpLevels.CHEATING))
            .then(CommandManager.argument("world", DimensionArgumentType.dimension())
                .then(CommandManager.literal("teleport")
                    .then(CommandManager.argument("target", EntityArgumentType.entities())
                        .executes(WorldCommand::teleportEntitiesTo)
                    )
                    .executes(WorldCommand::teleportSelfTo)
                )
                .then(CommandManager.literal("setspawn")
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .executes(WorldCommand::updateServerSpawnToPos)
                    )
                    .executes(WorldCommand::updateServerSpawnToPlayer)
                )
                .then(gamerules)
            )
        );
    }
    
    private static int teleportSelfTo(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return WorldCommand.teleportEntitiesTo(context, Collections.singleton(context.getSource().getEntity()));
    }
    private static int teleportEntitiesTo(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return WorldCommand.teleportEntitiesTo(context, EntityArgumentType.getEntities(context, "target"));
    }
    private static int teleportEntitiesTo(@NotNull CommandContext<ServerCommandSource> context, @NotNull Collection<? extends Entity> entities) throws CommandSyntaxException {
        ServerWorld world = DimensionArgumentType.getDimensionArgument(context, "world");
        
        // Teleport each entity to the world spawn
        for (Entity entity : entities)
            WarpUtils.teleportEntity(world, entity);
        
        // Return success
        return Command.SINGLE_SUCCESS;
    }
    
    private static int updateServerSpawnToPlayer(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return WorldCommand.updateServerSpawn(
            source.getServer(),
            DimensionArgumentType.getDimensionArgument(context, "world"),
            new BlockPos(source.getPosition())
        );
    }
    private static int updateServerSpawnToPos(@NotNull CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        return WorldCommand.updateServerSpawn(
            source.getServer(),
            DimensionArgumentType.getDimensionArgument(context, "world"),
            BlockPosArgumentType.getBlockPos(context, "pos")
        );
    }
    private static int updateServerSpawn(@NotNull MinecraftServer server, @NotNull ServerWorld world, @NotNull BlockPos pos) throws CommandSyntaxException {
        try {
            SewConfig.set(SewConfig.DEFAULT_WORLD, world.getRegistryKey());
            SewConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        
        server.getOverworld()
            .setSpawnPos(pos, 0.0F);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static <T extends GameRules.Rule<T>> int setGameRule(@NotNull CommandContext<ServerCommandSource> context, GameRules.Key<T> key) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        T rule = DimensionArgumentType.getDimensionArgument(context, "world")
            .getGameRules()
            .get(key);
        
        rule.set(context, "value");
        source.sendFeedback(new TranslatableText("commands.gamerule.set", key.getName(), rule.toString()), true);
        
        return rule.getCommandResult();
    }
    private static <T extends GameRules.Rule<T>> int queryGameRule(@NotNull CommandContext<ServerCommandSource> context, GameRules.Key<T> key) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        T rule = DimensionArgumentType.getDimensionArgument(context, "world")
            .getGameRules()
            .get(key);
        
        source.sendFeedback(new TranslatableText("commands.gamerule.query", key.getName(), rule.toString()), false);
        
        return rule.getCommandResult();
    }
    
}
