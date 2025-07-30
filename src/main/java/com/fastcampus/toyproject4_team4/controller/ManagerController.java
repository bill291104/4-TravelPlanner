package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @PostMapping("/{domain}")
    @ResponseBody
    public ResponseEntity<Void> embedding(@PathVariable String domain, @RequestParam Integer pk) {
        managerService.embedding(domain, pk);
        return  ResponseEntity.ok().build();
    }

    @PostMapping("/{domain}/batch")
    @ResponseBody
    public ResponseEntity<Void> embeddingBatch(@PathVariable String domain, @RequestParam List<Integer> pks) {
        managerService.embeddingBatch(domain, pks);
        return  ResponseEntity.ok().build();
    }

    @GetMapping("/main/{domain}")
    public String getAllMain(@PathVariable String domain, Pageable pageable, Model model) {
        Page<?> result = managerService.getAllMain(domain, pageable);
        model.addAttribute("page", result);
        model.addAttribute("domain", domain);
        return "manager/manager";
    }

    @GetMapping("/sub/{domain}/{pk}")
    public String getAllSub(@PathVariable String domain, @PathVariable Integer pk, Pageable pageable, Model model) {
        Page<?> result = managerService.getAllSub(domain, pk, pageable);
        model.addAttribute("page", result);
        model.addAttribute("domain", domain);
        model.addAttribute("mainPk", pk);
        return "manager/manager";
    }

    @PatchMapping("/{domain}")
    @ResponseBody
    public ResponseEntity<Void> update(@PathVariable String domain, @RequestParam Integer pk) {
        managerService.delete(domain, pk);
        managerService.embedding(domain, pk);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{domain}/batch")
    @ResponseBody
    public ResponseEntity<Void> updateBatch(@PathVariable String domain, @RequestParam List<Integer> pks) {
        managerService.deleteBatch(domain, pks);
        managerService.embeddingBatch(domain, pks);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{domain}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable String domain, @RequestParam Integer pk) {
        managerService.delete(domain, pk);
        return  ResponseEntity.ok().build();
    }

    @DeleteMapping("/{domain}/batch")
    @ResponseBody
    public  ResponseEntity<Void> deleteBatch(@PathVariable String domain, @RequestParam List<Integer> pks) {
        managerService.deleteBatch(domain, pks);
        return  ResponseEntity.ok().build();
    }
}
