package com.tacs.tp1c2026.commands;

public interface CommandHandler {
    String name();
    String description();
    String execute(CommandContext ctx);
}
