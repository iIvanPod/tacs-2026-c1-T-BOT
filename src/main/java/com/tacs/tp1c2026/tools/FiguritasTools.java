package com.tacs.tp1c2026.tools;

import com.tacs.tp1c2026.client.BackendApiClient;
import com.tacs.tp1c2026.dtos.Auction;
import com.tacs.tp1c2026.dtos.Card;
import com.tacs.tp1c2026.dtos.CollectionCard;
import com.tacs.tp1c2026.dtos.MissingCard;
import com.tacs.tp1c2026.dtos.TradePublication;
import com.tacs.tp1c2026.session.NotLoggedInException;
import com.tacs.tp1c2026.session.Session;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Herramientas (@Tool) que el agente conversacional puede invocar según la intención del usuario.
 * Cada operación autenticada toma la sesión (userId + token) desde el {@link ToolContext},
 * que Spring AI inyecta fuera de banda: el modelo nunca ve ni controla las credenciales.
 *
 * No se expone login como tool a propósito: la contraseña no debe pasar por el modelo (el login ocurre en la Mini App que abre /login).
 */
@Component
public class FiguritasTools {

    /** Clave bajo la cual el agente coloca la {@link Session} en el ToolContext. */
    public static final String SESSION_KEY = "session";

    /** Tope de resultados que se devuelven al modelo, para no inflar tokens con todo el catálogo. */
    private static final int MAX_RESULTADOS = 50;

    private final BackendApiClient apiClient;

    public FiguritasTools(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Tool(description = "Busca figuritas en el catálogo filtrando por equipo, país y/o número. "
            + "Pasá al menos un filtro; si te piden el catálogo completo, sugerí el comando /catalogo. "
            + "Devuelve como máximo 50 resultados.")
    public List<Card> buscarEnCatalogo(
            @ToolParam(required = false, description = "Equipo de la figurita (ej. Argentina, Boca)") String equipo,
            @ToolParam(required = false, description = "País de la figurita") String pais,
            @ToolParam(required = false, description = "Número de la figurita en el catálogo") Integer numero,
            ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.getCatalog(session.token()).stream()
                .filter(c -> contiene(c.team(), equipo))
                .filter(c -> contiene(c.country(), pais))
                .filter(c -> numero == null || numero.equals(c.number()))
                .limit(MAX_RESULTADOS)
                .toList();
    }

    @Tool(description = "Devuelve el detalle de una figurita del catálogo a partir de su id.")
    public Card verFigurita(
            @ToolParam(description = "Id de la figurita en el catálogo") String figuritaId,
            ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.getCardById(figuritaId, session.token());
    }

    @Tool(description = "Devuelve las figuritas que el usuario YA tiene en su colección, con la cantidad de cada una.")
    public List<CollectionCard> verColeccion(ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.getCollection(session.userId(), session.token());
    }

    @Tool(description = "Agrega una figurita (por id) a la colección del usuario o incrementa su cantidad. Devuelve la figurita con la cantidad actualizada.")
    public CollectionCard agregarAColeccion(
            @ToolParam(description = "Id de la figurita a agregar") String figuritaId,
            ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.addToCollection(session.userId(), figuritaId, session.token());
    }

    @Tool(description = "Quita o decrementa en una unidad una figurita de la colección del usuario. Es una acción destructiva: confirmá con el usuario antes de usarla.")
    public String quitarDeColeccion(
            @ToolParam(description = "Id de la figurita a quitar de la colección") String figuritaId,
            ToolContext toolContext) {
        Session session = requireSession(toolContext);
        apiClient.decrementFromCollection(session.userId(), figuritaId, session.token());
        return "Se quitó una unidad de la figurita " + figuritaId + " de la colección.";
    }

    @Tool(description = "Devuelve las figuritas que al usuario le faltan (las que marcó como faltantes).")
    public List<MissingCard> verFaltantes(ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.getMissingCards(session.userId(), session.token());
    }

    @Tool(description = "Marca una figurita (por id) como faltante para el usuario.")
    public MissingCard marcarFaltante(
            @ToolParam(description = "Id de la figurita a marcar como faltante") String figuritaId,
            ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.addMissingCard(session.userId(), figuritaId, session.token());
    }

    @Tool(description = "Quita una figurita de la lista de faltantes del usuario. Es una acción destructiva: confirmá con el usuario antes de usarla.")
    public String quitarFaltante(
            @ToolParam(description = "Id de la figurita a quitar de los faltantes") String figuritaId,
            ToolContext toolContext) {
        Session session = requireSession(toolContext);
        apiClient.removeMissingCard(session.userId(), figuritaId, session.token());
        return "Se quitó la figurita " + figuritaId + " de la lista de faltantes.";
    }

    @Tool(description = "Crea una publicación de intercambio de una figurita repetida (por id), indicando cuántas unidades ofrecés. "
            + "Es una acción que crea algo: confirmá con el usuario antes de ejecutarla. Devuelve la publicación creada.")
    public TradePublication publicarFiguritaRepetida(
            @ToolParam(description = "Id de la figurita repetida a publicar") String cardId,
            @ToolParam(description = "Cantidad de unidades repetidas a publicar") Integer cantidad,
            ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.createTradePublication(cardId, cantidad, session.token());
    }

    @Tool(description = "Lista las publicaciones de intercambio activas de OTROS usuarios (no las propias).")
    public List<TradePublication> verPublicaciones(ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.listPublications(1, 20, session.token()).items();
    }

    @Tool(description = "Lista las subastas activas de OTROS usuarios (no las propias).")
    public List<Auction> verSubastas(ToolContext toolContext) {
        Session session = requireSession(toolContext);
        return apiClient.listAuctions(1, 20, session.token()).items();
    }

    private static boolean contiene(String valor, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        return valor != null && valor.toLowerCase().contains(filtro.toLowerCase().trim());
    }

    private Session requireSession(ToolContext toolContext) {
        Object value = toolContext.getContext().get(SESSION_KEY);
        if (!(value instanceof Session session)) {
            throw new NotLoggedInException("El usuario no inició sesión.");
        }
        return session;
    }
}
