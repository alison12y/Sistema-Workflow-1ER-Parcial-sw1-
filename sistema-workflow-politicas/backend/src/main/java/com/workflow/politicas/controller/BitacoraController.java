package com.workflow.politicas.controller;

import com.workflow.politicas.dto.BitacoraFilterRequest;
import com.workflow.politicas.dto.BitacoraResponse;
import com.workflow.politicas.service.BitacoraService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bitacora")
public class BitacoraController {

    private final BitacoraService bitacoraService;

    public BitacoraController(BitacoraService bitacoraService) {
        this.bitacoraService = bitacoraService;
    }

    @GetMapping
    public List<BitacoraResponse> getAll(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        if (hasFilters(userId, username, module, action, dateFrom, dateTo)) {
            return bitacoraService.findWithFilters(
                    buildFilter(userId, username, module, action, dateFrom, dateTo)
            );
        }
        return bitacoraService.findAll();
    }

    @GetMapping("/export/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        String csv = bitacoraService.exportCsv(
                buildFilter(userId, username, module, action, dateFrom, dateTo)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bitacora.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/module/{module}")
    public List<BitacoraResponse> getByModule(@PathVariable String module) {
        return bitacoraService.findByModule(module);
    }

    @GetMapping("/user/{userId}")
    public List<BitacoraResponse> getByUser(@PathVariable String userId) {
        return bitacoraService.findByUserId(userId);
    }

    private static boolean hasFilters(
            String userId,
            String username,
            String module,
            String action,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        return (userId != null && !userId.isBlank())
                || (username != null && !username.isBlank())
                || (module != null && !module.isBlank())
                || (action != null && !action.isBlank())
                || dateFrom != null
                || dateTo != null;
    }

    private static BitacoraFilterRequest buildFilter(
            String userId,
            String username,
            String module,
            String action,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        BitacoraFilterRequest filter = new BitacoraFilterRequest();
        filter.setUserId(userId);
        filter.setUsername(username);
        filter.setModule(module);
        filter.setAction(action);
        filter.setDateFrom(dateFrom);
        filter.setDateTo(dateTo);
        return filter;
    }
}
