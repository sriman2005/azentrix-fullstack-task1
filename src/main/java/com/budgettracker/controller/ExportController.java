package com.budgettracker.controller;

import com.budgettracker.security.UserPrincipal;
import com.budgettracker.service.ExportService;
import com.budgettracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired private ExportService exportService;
    @Autowired private UserService userService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        String name = userService.getUser(p.getId()).getFullName();
        byte[] pdf = exportService.exportPdf(p.getId(), startDate, endDate, name);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("transactions.pdf").build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        byte[] csv = exportService.exportCsv(p.getId(), startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("transactions.csv").build());
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        byte[] excel = exportService.exportExcel(p.getId(), startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("transactions.xlsx").build());
        return new ResponseEntity<>(excel, headers, HttpStatus.OK);
    }
}
