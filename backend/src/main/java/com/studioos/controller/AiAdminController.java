package com.studioos.controller;

import com.studioos.ai.dto.AiDtos.AiChatFaqDto;
import com.studioos.ai.dto.AiDtos.AiChatLeadDto;
import com.studioos.ai.dto.AiDtos.AiChatOfferDto;
import com.studioos.ai.dto.AiDtos.AiChatPolicyDto;
import com.studioos.ai.dto.AiDtos.AiClassDto;
import com.studioos.ai.dto.AiDtos.AiClassScheduleDto;
import com.studioos.ai.dto.AiDtos.AiPackageDto;
import com.studioos.ai.dto.AiDtos.AiStudioSettingDto;
import com.studioos.ai.dto.AiDtos.AnalyticsDto;
import com.studioos.ai.service.AiAnalyticsService;
import com.studioos.ai.service.AiKnowledgeService;
import com.studioos.ai.service.AiLeadService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated staff API for AI knowledge, leads and analytics.
 * Branch-scoping follows the existing staff conventions (branch query resolves
 * through AiBranchContext; RoleScopeFilter continues to gate developer/instructor
 * access, with GETs on this path allowed read-only — see SecurityConfiguration).
 */
@RestController @RequestMapping("/api/ai/admin")
public class AiAdminController {
    private final AiKnowledgeService knowledge;
    private final AiLeadService leads;
    private final AiAnalyticsService analytics;

    public AiAdminController(AiKnowledgeService knowledge, AiLeadService leads, AiAnalyticsService analytics) {
        this.knowledge = knowledge; this.leads = leads; this.analytics = analytics;
    }

    // ---- classes ----
    @GetMapping("/classes") public List<AiClassDto> classes(@RequestParam String branch) { return knowledge.listClasses("default", branch); }
    @PostMapping("/classes") public ResponseEntity<AiClassDto> saveClass(@Valid @RequestBody AiClassDto dto) {
        AiClassDto saved = knowledge.saveClass("default", dto);
        return ResponseEntity.created(URI.create("/api/ai/admin/classes/" + saved.id())).body(saved);
    }
    @DeleteMapping("/classes/{id}") public ResponseEntity<Void> deleteClass(@RequestParam String branch, @PathVariable Long id) {
        knowledge.deleteClass("default", branch, id); return ResponseEntity.noContent().build();
    }

    // ---- schedules ----
    @GetMapping("/schedules") public List<AiClassScheduleDto> schedules(@RequestParam String branch) { return knowledge.listSchedules("default", branch); }
    @PostMapping("/schedules") public ResponseEntity<AiClassScheduleDto> saveSchedule(@Valid @RequestBody AiClassScheduleDto dto) {
        AiClassScheduleDto saved = knowledge.saveSchedule("default", dto);
        return ResponseEntity.created(URI.create("/api/ai/admin/schedules/" + saved.id())).body(saved);
    }
    @DeleteMapping("/schedules/{id}") public ResponseEntity<Void> deleteSchedule(@RequestParam String branch, @PathVariable Long id) {
        knowledge.deleteSchedule("default", branch, id); return ResponseEntity.noContent().build();
    }

    // ---- packages ----
    @GetMapping("/packages") public List<AiPackageDto> packages(@RequestParam String branch) { return knowledge.listPackages("default", branch); }
    @PostMapping("/packages") public ResponseEntity<AiPackageDto> savePackage(@Valid @RequestBody AiPackageDto dto) {
        AiPackageDto saved = knowledge.savePackage("default", dto);
        return ResponseEntity.created(URI.create("/api/ai/admin/packages/" + saved.id())).body(saved);
    }
    @DeleteMapping("/packages/{id}") public ResponseEntity<Void> deletePackage(@RequestParam String branch, @PathVariable Long id) {
        knowledge.deletePackage("default", branch, id); return ResponseEntity.noContent().build();
    }

    // ---- settings (incl. admission fee) ----
    @GetMapping("/settings") public List<AiStudioSettingDto> settings(@RequestParam String branch) { return knowledge.listSettings("default", branch); }
    @PostMapping("/settings") public ResponseEntity<AiStudioSettingDto> saveSetting(@Valid @RequestBody AiStudioSettingDto dto) {
        AiStudioSettingDto saved = knowledge.saveSetting("default", dto);
        return ResponseEntity.created(URI.create("/api/ai/admin/settings/" + saved.id())).body(saved);
    }
    @DeleteMapping("/settings/{id}") public ResponseEntity<Void> deleteSetting(@RequestParam String branch, @PathVariable Long id) {
        knowledge.deleteSetting("default", branch, id); return ResponseEntity.noContent().build();
    }

    // ---- faqs ----
    @GetMapping("/faqs") public List<AiChatFaqDto> faqs(@RequestParam String branch) { return knowledge.listFaqs("default", branch); }
    @PostMapping("/faqs") public ResponseEntity<AiChatFaqDto> saveFaq(@Valid @RequestBody AiChatFaqDto dto) {
        AiChatFaqDto saved = knowledge.saveFaq("default", dto);
        return ResponseEntity.created(URI.create("/api/ai/admin/faqs/" + saved.id())).body(saved);
    }
    @DeleteMapping("/faqs/{id}") public ResponseEntity<Void> deleteFaq(@RequestParam String branch, @PathVariable Long id) {
        knowledge.deleteFaq("default", branch, id); return ResponseEntity.noContent().build();
    }

    // ---- policies ----
    @GetMapping("/policies") public List<AiChatPolicyDto> policies(@RequestParam String branch) { return knowledge.listPolicies("default", branch); }
    @PostMapping("/policies") public ResponseEntity<AiChatPolicyDto> savePolicy(@Valid @RequestBody AiChatPolicyDto dto) {
        AiChatPolicyDto saved = knowledge.savePolicy("default", dto);
        return ResponseEntity.created(URI.create("/api/ai/admin/policies/" + saved.id())).body(saved);
    }
    @DeleteMapping("/policies/{id}") public ResponseEntity<Void> deletePolicy(@RequestParam String branch, @PathVariable Long id) {
        knowledge.deletePolicy("default", branch, id); return ResponseEntity.noContent().build();
    }

    // ---- offers ----
    @GetMapping("/offers") public List<AiChatOfferDto> offers(@RequestParam String branch) { return knowledge.listOffers("default", branch); }
    @PostMapping("/offers") public ResponseEntity<AiChatOfferDto> saveOffer(@Valid @RequestBody AiChatOfferDto dto) {
        AiChatOfferDto saved = knowledge.saveOffer("default", dto);
        return ResponseEntity.created(URI.create("/api/ai/admin/offers/" + saved.id())).body(saved);
    }
    @DeleteMapping("/offers/{id}") public ResponseEntity<Void> deleteOffer(@RequestParam String branch, @PathVariable Long id) {
        knowledge.deleteOffer("default", branch, id); return ResponseEntity.noContent().build();
    }

    // ---- leads inbox ----
    @GetMapping("/leads") public List<AiChatLeadDto> leadInbox(@RequestParam String branch, @RequestParam(defaultValue = "ALL") String status) {
        return leads.inbox("default", branch, status);
    }
    @PatchMapping("/leads/{id}") public AiChatLeadDto leadStatus(@RequestParam String branch, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return leads.setStatus("default", branch, id, body.get("status"));
    }

    // ---- analytics ----
    @GetMapping("/analytics") public AnalyticsDto analytics(@RequestParam String branch) { return analytics.summary("default", branch); }

    // ---- diagnostics ----
    @GetMapping("/diagnostics") public java.util.Map<String, Object> diagnostics(@RequestParam String branch) {
        return knowledge.diagnostics("default", branch);
    }
}
