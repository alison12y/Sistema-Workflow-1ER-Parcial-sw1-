package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditResult;
import com.workflow.politicas.dto.BitacoraFilterRequest;
import com.workflow.politicas.dto.BitacoraResponse;
import com.workflow.politicas.model.Bitacora;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.BitacoraRepository;
import com.workflow.politicas.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Servicio centralizado de auditoría / bitácora del sistema (CU11).
 * Todos los módulos deben registrar eventos a través de este servicio.
 */
@Service
public class BitacoraService {

    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BitacoraRepository bitacoraRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public BitacoraService(
            BitacoraRepository bitacoraRepository,
            UserRepository userRepository,
            MongoTemplate mongoTemplate
    ) {
        this.bitacoraRepository = bitacoraRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void registrar(String module, String action, String description, String entityType, String entityId) {
        registrarExito(resolveCurrentUsername(), module, action, description, entityType, entityId, null);
    }

    public void registrar(
            String actorUsername,
            String module,
            String action,
            String description,
            String entityType,
            String entityId
    ) {
        registrarExito(actorUsername, module, action, description, entityType, entityId, null);
    }

    public void registrarExito(
            String actorUsername,
            String module,
            String action,
            String description,
            String entityType,
            String entityId,
            String clientIp
    ) {
        persist(resolveActorLogin(actorUsername), module, action, description, entityType, entityId, AuditResult.EXITO, clientIp);
    }

    public void registrarError(
            String actorUsername,
            String module,
            String action,
            String description,
            String entityType,
            String entityId,
            String clientIp
    ) {
        persist(resolveActorLogin(actorUsername), module, action, description, entityType, entityId, AuditResult.ERROR, clientIp);
    }

    private String resolveActorLogin(String actorUsername) {
        if (actorUsername != null && !actorUsername.isBlank()) {
            return actorUsername.trim();
        }
        return resolveCurrentUsername();
    }

    private void persist(
            String actorUsername,
            String module,
            String action,
            String description,
            String entityType,
            String entityId,
            AuditResult resultado,
            String clientIp
    ) {
        String login = actorUsername != null && !actorUsername.isBlank() ? actorUsername.trim() : "system";
        String userId = login;
        String fullName = login;

        Optional<User> userOpt = userRepository.findByUsername(login);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(login.toLowerCase(Locale.ROOT));
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userId = user.getId();
            login = user.getUsername();
            fullName = displayName(user);
        }

        Bitacora entry = new Bitacora();
        entry.setUserId(userId);
        entry.setUsername(login);
        entry.setFullName(fullName);
        entry.setModule(module);
        entry.setAction(action);
        entry.setDescription(sanitizeDescription(description));
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setResultado(resultado.name());
        entry.setIp(clientIp != null && !clientIp.isBlank() ? clientIp : resolveClientIp());
        entry.setCreatedAt(LocalDateTime.now());
        bitacoraRepository.save(entry);
    }

    public String resolveActorDisplay() {
        return resolveActorDisplay(resolveCurrentUsername());
    }

    public String resolveActorDisplay(String username) {
        if (username == null || username.isBlank()) {
            return "Sistema";
        }
        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT));
        }
        return userOpt.map(this::displayName).orElse(username.trim());
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return resolveClientIp();
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public List<BitacoraResponse> findAll() {
        return bitacoraRepository.findAll().stream()
                .sorted(Comparator.comparing(Bitacora::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public List<BitacoraResponse> findByModule(String module) {
        return bitacoraRepository.findByModule(module).stream()
                .sorted(Comparator.comparing(Bitacora::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public List<BitacoraResponse> findByUserId(String userId) {
        return bitacoraRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Bitacora::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public List<BitacoraResponse> findWithFilters(BitacoraFilterRequest filter) {
        Query query = buildFilterQuery(filter);
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Bitacora.class).stream()
                .map(this::toResponse)
                .toList();
    }

    public String exportCsv(BitacoraFilterRequest filter) {
        List<BitacoraResponse> rows = findWithFilters(filter);
        StringBuilder csv = new StringBuilder();
        csv.append("fecha,usuario,username,modulo,accion,descripcion,entidad,entidadId,resultado,ip\n");
        for (BitacoraResponse row : rows) {
            csv.append(escapeCsv(formatDate(row.getCreatedAt()))).append(',');
            csv.append(escapeCsv(displayUser(row))).append(',');
            csv.append(escapeCsv(row.getUsername())).append(',');
            csv.append(escapeCsv(row.getModule())).append(',');
            csv.append(escapeCsv(row.getAction())).append(',');
            csv.append(escapeCsv(row.getDescription())).append(',');
            csv.append(escapeCsv(row.getEntityType())).append(',');
            csv.append(escapeCsv(row.getEntityId())).append(',');
            csv.append(escapeCsv(row.getResultado())).append(',');
            csv.append(escapeCsv(row.getIp())).append('\n');
        }
        return csv.toString();
    }

    public void deleteByEntity(String entityType, String entityId) {
        if (entityType == null || entityType.isBlank() || entityId == null || entityId.isBlank()) {
            return;
        }
        bitacoraRepository.deleteByEntityTypeAndEntityId(entityType.trim(), entityId.trim());
    }

    private Query buildFilterQuery(BitacoraFilterRequest filter) {
        List<Criteria> criteria = new ArrayList<>();
        if (filter != null) {
            if (filter.getUserId() != null && !filter.getUserId().isBlank()) {
                criteria.add(Criteria.where("userId").is(filter.getUserId().trim()));
            }
            if (filter.getUsername() != null && !filter.getUsername().isBlank()) {
                String term = filter.getUsername().trim();
                criteria.add(new Criteria().orOperator(
                        Criteria.where("username").regex(term, "i"),
                        Criteria.where("fullName").regex(term, "i")
                ));
            }
            if (filter.getModule() != null && !filter.getModule().isBlank()) {
                criteria.add(Criteria.where("module").is(filter.getModule().trim()));
            }
            if (filter.getAction() != null && !filter.getAction().isBlank()) {
                criteria.add(Criteria.where("action").is(filter.getAction().trim()));
            }
            if (filter.getDateFrom() != null) {
                criteria.add(Criteria.where("createdAt").gte(filter.getDateFrom()));
            }
            if (filter.getDateTo() != null) {
                criteria.add(Criteria.where("createdAt").lte(filter.getDateTo()));
            }
        }
        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }
        return query;
    }

    private BitacoraResponse toResponse(Bitacora entry) {
        BitacoraResponse response = new BitacoraResponse();
        response.setId(entry.getId());
        response.setUserId(entry.getUserId());
        response.setUsername(entry.getUsername());
        response.setFullName(resolveFullName(entry));
        response.setModule(entry.getModule());
        response.setAction(entry.getAction());
        response.setDescription(entry.getDescription());
        response.setEntityType(entry.getEntityType());
        response.setEntityId(entry.getEntityId());
        response.setResultado(entry.getResultado() != null ? entry.getResultado() : AuditResult.EXITO.name());
        response.setIp(entry.getIp());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }

    private static String resolveFullName(Bitacora entry) {
        if (entry.getFullName() != null && !entry.getFullName().isBlank()) {
            return entry.getFullName().trim();
        }
        return entry.getUsername();
    }

    private static String displayUser(BitacoraResponse row) {
        if (row.getFullName() != null && !row.getFullName().isBlank()) {
            return row.getFullName().trim();
        }
        return row.getUsername() != null ? row.getUsername() : "";
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "system";
        }
        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(String.valueOf(principal))) {
            return "system";
        }
        if (principal instanceof UserDetails details) {
            return details.getUsername();
        }
        return auth.getName();
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return resolveClientIp(attrs.getRequest());
            }
        } catch (Exception ignored) {
            // sin contexto HTTP
        }
        return null;
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUsername();
    }

    private static String sanitizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description
                .replaceAll("(?i)password\\s*[:=]\\s*\\S+", "password=[REDACTED]")
                .replaceAll("(?i)bearer\\s+\\S+", "Bearer [REDACTED]")
                .replaceAll("(?i)token\\s*[:=]\\s*\\S+", "token=[REDACTED]");
    }

    private static String formatDate(LocalDateTime value) {
        return value != null ? value.format(CSV_DATE) : "";
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
