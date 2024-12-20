package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import epicode.it.dao.biglietto.BigliettoDAO;
import epicode.it.dao.mezzo.AutobusDAO;
import epicode.it.dao.mezzo.MezzoDAO;
import epicode.it.dao.mezzo.TramDAO;
import epicode.it.dao.rivenditore.RivenditoreDAO;
import epicode.it.dao.tessera.TesseraDAO;
import epicode.it.dao.utente.UtenteDAO;
import epicode.it.entities.biglietto.Biglietto;
import epicode.it.entities.mezzo.Autobus;
import epicode.it.entities.mezzo.Mezzo;
import epicode.it.entities.mezzo.Tram;
import epicode.it.entities.rivenditore.RivAutomatico;
import epicode.it.entities.rivenditore.RivFisico;
import epicode.it.entities.rivenditore.Rivenditore;
import epicode.it.entities.tessera.Tessera;
import epicode.it.entities.utente.Utente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HandleUtenti implements HttpHandler {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("unit-jpa");
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Per il parsing JSON

    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // Nessun contenuto
            return;
        }

        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET":
                handleGet(exchange);
                break;
            case "POST":
                handlePost(exchange);
                break;
            case "PUT":
                handlePut(exchange);
                break;
            case "DELETE":
                handleDelete(exchange);
                break;
            default:
                exchange.sendResponseHeaders(405, -1); // Metodo non consentito
        }


    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        EntityManager em = emf.createEntityManager();
        UtenteDAO dao = new UtenteDAO(em);

        try {
            // Estrai l'ID dal percorso
            String path = exchange.getRequestURI().getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length < 3) {
                exchange.sendResponseHeaders(400, -1); // ID non fornito
                return;
            }

            Long id = Long.parseLong(pathParts[2]);
            System.out.println(id);

            // Verifica se la Tratta esiste
            Utente biglietto = dao.getById(id);
            if (biglietto == null) {
                exchange.sendResponseHeaders(404, -1); // Tratta non trovata
                return;
            }


            dao.delete(biglietto);
            

            // Risposta al client
            exchange.sendResponseHeaders(200, -1); // Eliminazione riuscita
        } catch (Exception e) {
            e.printStackTrace(); // Log dell'errore per debugging
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            exchange.sendResponseHeaders(500, -1); // Errore interno del server
        } finally {
            em.close(); // Assicurati di chiudere l'EntityManager
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        EntityManager em = emf.createEntityManager();
        UtenteDAO utenteDAO = new UtenteDAO(em);

        try {
            // Leggi il corpo della richiesta come JSON
            Map<String, Object> requestData = objectMapper.readValue(exchange.getRequestBody(), Map.class);

            // Creazione di un nuovo Utente
            Utente utente = new Utente();
            utente.setNome((String) requestData.get("nome"));
            utente.setCognome((String) requestData.get("cognome"));
            utente.setEmail((String) requestData.get("email"));
            // Parsing della data di nascita
            String dataNascitaString = (String) requestData.get("dataNascita");
            if (dataNascitaString != null) {
                utente.setDataNascita(LocalDate.parse(dataNascitaString));
            }
            utente.setRuolo((String) requestData.get("ruolo"));

            // Associa una tessera se fornita
            Map<String, Object> tesseraData = (Map<String, Object>) requestData.get("tessera");
            if (tesseraData != null) {
                Long tesseraId = Long.parseLong(tesseraData.get("id").toString());
                TesseraDAO tesseraDAO = new TesseraDAO(em);
                Tessera tessera = tesseraDAO.getById(tesseraId);

                if (tessera != null) {
                    utente.setTessera(tessera);
                } else {
                    exchange.sendResponseHeaders(404, -1); // Tessera non trovata
                    return;
                }
            }

            // Salva l'Utente
            utenteDAO.save(utente);

            // Risposta al client
            exchange.sendResponseHeaders(201, -1); // Creato con successo
        } catch (Exception e) {
            e.printStackTrace(); // Log dell'errore per debugging
            exchange.sendResponseHeaders(400, -1); // Errore nella richiesta
        } finally {
            em.close(); // Assicurati di chiudere l'EntityManager
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        EntityManager em = emf.createEntityManager();
        UtenteDAO dao = new UtenteDAO(em);

        // Leggi il corpo della richiesta come JSON
        Map<String, Object> requestData = objectMapper.readValue(exchange.getRequestBody(), Map.class);
        Long id = Long.parseLong(requestData.get("id").toString());

        Utente utente = dao.getById(id);
        if (utente == null) {
            exchange.sendResponseHeaders(404, -1); // Rivenditore non trovato
            em.close();
            return;
        }

        utente.setNome((String) requestData.get("nome"));
        utente.setCognome((String) requestData.get("cognome"));
        utente.setEmail((String) requestData.get("email"));
        // Parsing della data di nascita
        String dataNascitaString = (String) requestData.get("dataNascita");
        if (dataNascitaString != null) {
            utente.setDataNascita(LocalDate.parse(dataNascitaString));
        }
        utente.setRuolo((String) requestData.get("ruolo"));
        dao.update(utente);

        em.close();
        exchange.sendResponseHeaders(200, -1); // Aggiornamento riuscito
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        EntityManager em = emf.createEntityManager();
        UtenteDAO dao = new UtenteDAO(em);

        // Recupera tutti i rivenditori
        List<Utente> utenti = dao.getAll();
        em.close();

        String jsonResponse = utenti.stream()
                .map(u -> {
                    // Controllo se Tessera è presente
                    String tesseraJson = u.getTessera() != null
                            ? String.format("{\"id\":%d,\"codice\":\"%s\"}", u.getTessera().getId(), u.getTessera().getCodice())
                            : null;

                    // Creazione del JSON completo dell'Utente
                    return String.format(
                            "{\"id\":%d,\"nome\":\"%s\",\"cognome\":\"%s\",\"dataNascita\":\"%s\",\"email\":\"%s\",\"tessera\":%s,\"ruolo\":\"%s\"}",
                            u.getId(),
                            u.getNome(),
                            u.getCognome(),
                            u.getDataNascita() != null ? u.getDataNascita().toString() : null,
                            u.getEmail(),
                            tesseraJson,
                            u.getRuolo() // Aggiungi la proprietà ruolo
                    );
                })
                .collect(Collectors.joining(",", "[", "]"));


        // Restituisci la risposta
        exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes());
        }
    }
}
