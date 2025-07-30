package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final ManagerService managerService;

    @GetMapping
    public String managerPage() {
        return "manager/manager";
    }

    @ResponseBody
    @PostMapping("/{domain}")
    public ResponseEntity<Void> embedding(@PathVariable Domains domain, @RequestParam Long pk) {
        managerService.embedding(domain, pk);
        return  ResponseEntity.ok().build();
    }

    @ResponseBody
    @PostMapping("/{domain}/batch")
    public ResponseEntity<Void> embeddingBatch(@PathVariable Domains domain, @RequestParam List<Long> pks) {
        managerService.embeddingBatch(domain, pks);
        return  ResponseEntity.ok().build();
    }

    @ResponseBody
    @GetMapping("/main/{domain}")
    public ResponseEntity<Page<?>> getAllMain(@PathVariable Domains domain, Pageable pageable) {
        Page<?> result = managerService.getAllMain(domain, pageable);
        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @GetMapping("/sub/{domain}/{pk}")
    public ResponseEntity<Page<?>> getAllSub(@PathVariable Domains domain, @PathVariable Long pk, Pageable pageable) {
        Page<?> result = managerService.getAllSub(domain, pk, pageable);
        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @PatchMapping("/{domain}")
    public ResponseEntity<Void> update(@PathVariable Domains domain, @RequestParam Long pk) {
        managerService.delete(domain, pk);
        managerService.embedding(domain, pk);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @PatchMapping("/{domain}/batch")
    public ResponseEntity<Void> updateBatch(@PathVariable Domains domain, @RequestParam List<Long> pks) {
        managerService.deleteBatch(domain, pks);
        managerService.embeddingBatch(domain, pks);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/{domain}")
    public ResponseEntity<Void> delete(@PathVariable Domains domain, @RequestParam Long pk) {
        managerService.delete(domain, pk);
        return  ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/{domain}/batch")
    public  ResponseEntity<Void> deleteBatch(@PathVariable Domains domain, @RequestParam List<Long> pks) {
        managerService.deleteBatch(domain, pks);
        return  ResponseEntity.ok().build();
    }
}
