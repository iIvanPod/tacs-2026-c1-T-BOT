package com.tacs.tp1c2026.commands;

import com.tacs.tp1c2026.chatlink.ChatLinkStore;
import org.springframework.stereotype.Component;

@Component
public class LogoutCommand implements CommandHandler {

    private final ChatLinkStore chatLinkStore;

    public LogoutCommand(ChatLinkStore chatLinkStore) {
        this.chatLinkStore = chatLinkStore;
    }

    @Override
    public String name() {
        return "/olvidame";
    }

    @Override
    public String description() {
        return "Olvida tu identificación. Tu chat queda sin asociar a un usuario.";
    }

    @Override
    public String execute(CommandContext ctx) {
        if (chatLinkStore.getUserId(ctx.chatId()).isEmpty()) {
            return "Tu chat ya no estaba identificado.";
        }
        chatLinkStore.unlink(ctx.chatId());
        return "Listo, te olvidé. Identificate de nuevo con /yosoy <userId>.";
    }
}
