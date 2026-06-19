package com.tacs.tp1c2026.commands;

/**
 * Capacidad opcional para comandos que responden con un teclado inline y/o reaccionan
 * a los callbacks de sus botones. El {@link CommandDispatcher} la usa para enrutar tanto
 * el comando de texto como los callbacks ({@code <comando>:<arg>}).
 */
public interface InteractiveCommand {
    BotMessage executeInteractive(CommandContext ctx);
}
