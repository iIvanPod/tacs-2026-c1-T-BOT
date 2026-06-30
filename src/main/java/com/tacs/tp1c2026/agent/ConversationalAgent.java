package com.tacs.tp1c2026.agent;

import com.tacs.tp1c2026.session.Session;
import com.tacs.tp1c2026.tools.FiguritasTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Agente conversacional: interpreta lenguaje natural con Gemini y dispara las {@link FiguritasTools}
 * según la intención del usuario. Mantiene memoria de conversación por chatId de Telegram.
 *
 * La {@link Session} (userId + token) se pasa por toolContext fuera de banda: el modelo nunca la ve.
 */
@Component
public class ConversationalAgent {

	private static final int MAX_MESSAGES = 20;

	private static final String SYSTEM_PROMPT = """
			Sos el asistente del bot de figuritas de TACS, dentro de un chat de Telegram.
			Hablás en español rioplatense, en tono amable y con respuestas breves.

			Ayudás al usuario a gestionar sus figuritas: buscar en el catálogo, ver el detalle de una figurita,
			y ver/agregar/quitar figuritas de su colección o de su lista de faltantes.
			Para responder usá SIEMPRE las herramientas disponibles; nunca inventes figuritas, ids ni cantidades.
			Para CADA pedido volvé a llamar la herramienta correspondiente; nunca respondas de memoria ni
			reutilices una respuesta anterior.
			La colección (lo que el usuario TIENE, con cantidades) y los faltantes (lo que le FALTA) son cosas
			DISTINTAS: para la colección usá verColeccion y para los faltantes verFaltantes; no las confundas.

			Reglas:
			1. Antes de ejecutar una acción destructiva (quitar una figurita de la colección o de los faltantes)
			   o que crea algo (publicar una figurita repetida para intercambio), confirmá explícitamente con el
			   usuario y esperá que diga que sí antes de usar la herramienta.
			2. Si una herramienta indica que el usuario no inició sesión, pedile que use /login para iniciar sesión en la app.
			3. Si no te queda clara la intención o falta el id de una figurita, pedí una aclaración corta.
			4. Para buscar en el catálogo usá la herramienta con un filtro (equipo, país o número). Si te piden
			   el catálogo completo, sugerí el comando /catalogo en vez de listar todo.
			5. No reveles tokens, ids de usuario ni detalles técnicos internos.
			""";

	private final ChatClient chatClient;

	public ConversationalAgent(ChatClient.Builder chatClientBuilder, FiguritasTools tools) {
		ChatMemory chatMemory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(new InMemoryChatMemoryRepository())
				.maxMessages(MAX_MESSAGES)
				.build();

		this.chatClient = chatClientBuilder
				.defaultSystem(SYSTEM_PROMPT)
				.defaultTools(tools)
				.defaultAdvisors(
						MessageChatMemoryAdvisor.builder(chatMemory).build(),
						new SimpleLoggerAdvisor())
				.build();
	}

	/**
	 * Procesa un mensaje en lenguaje natural y devuelve la respuesta del agente.
	 *
	 * @param chatId   chat de Telegram, usado como id de conversación para la memoria
	 * @param userText texto del usuario
	 * @param session  sesión del usuario, o {@code null} si no inició sesión
	 */
	public String chat(long chatId, String userText, Session session) {
		// El toolContext nunca debe ir vacío: Spring AI rechaza ejecutar una tool con parámetro
		// ToolContext si el contexto está vacío. Por eso siempre incluimos el chatId (no se expone
		// al modelo) y, si hay sesión, la sesión. Sin sesión, la tool lanza NotLoggedInException.
		Map<String, Object> toolContext = new HashMap<>();
		toolContext.put("chatId", chatId);
		if (session != null) {
			toolContext.put(FiguritasTools.SESSION_KEY, session);
		}

		return chatClient.prompt()
				.user(userText)
				.toolContext(toolContext)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(chatId)))
				.call()
				.content();
	}
}
