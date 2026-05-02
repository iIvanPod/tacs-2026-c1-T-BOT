package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;

import java.util.Optional;

public abstract class IdentifiedCommand implements CommandHandler {

    private final ChatLinkStore chatLinkStore;

    protected IdentifiedCommand(ChatLinkStore chatLinkStore) {
        this.chatLinkStore = chatLinkStore;
    }

    @Override
    public final String execute(CommandContext ctx) {
        Optional<String> userId = chatLinkStore.getUserId(ctx.chatId());
        if (userId.isEmpty()) {
            return "Antes tenés que identificarte con /yosoy <userId>.";
        }
        return executeAsUser(userId.get(), ctx);
    }

    protected abstract String executeAsUser(String userId, CommandContext ctx);
}
