package com.workflow.politicas.controller;

import com.workflow.politicas.dto.BitacoraResponse;
import com.workflow.politicas.service.BitacoraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bitacora")
public class BitacoraController {

    private final BitacoraService bitacoraService;

    public BitacoraController(BitacoraService bitacoraService) {
        this.bitacoraService = bitacoraService;
    }

    @GetMapping
    public List<BitacoraResponse> getAll() {
        return bitacoraService.findAll();
    }

    @GetMapping("/module/{module}")
    public List<BitacoraResponse> getByModule(@PathVariable String module) {
        return bitacoraService.findByModule(module);
    }

    @GetMapping("/user/{userId}")
    public List<BitacoraResponse> getByUser(@PathVariable String userId) {
        return bitacoraService.findByUserId(userId);
    }
}
