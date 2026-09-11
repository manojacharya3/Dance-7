package com.studioos.ai.service;

import com.studioos.ai.dto.AiDtos.AiChatFaqDto;
import com.studioos.ai.dto.AiDtos.AiChatOfferDto;
import com.studioos.ai.dto.AiDtos.AiChatPolicyDto;
import com.studioos.ai.dto.AiDtos.AiClassDto;
import com.studioos.ai.dto.AiDtos.AiClassScheduleDto;
import com.studioos.ai.dto.AiDtos.AiPackageDto;
import com.studioos.ai.dto.AiDtos.AiStudioSettingDto;
import com.studioos.ai.model.AiChatFaq;
import com.studioos.ai.model.AiChatOffer;
import com.studioos.ai.model.AiChatPolicy;
import com.studioos.ai.model.AiClass;
import com.studioos.ai.model.AiClassSchedule;
import com.studioos.ai.model.AiPackage;
import com.studioos.ai.model.AiStudioSetting;
import com.studioos.ai.repository.AiChatFaqRepository;
import com.studioos.ai.repository.AiChatOfferRepository;
import com.studioos.ai.repository.AiChatPolicyRepository;
import com.studioos.ai.repository.AiClassRepository;
import com.studioos.ai.repository.AiClassScheduleRepository;
import com.studioos.ai.repository.AiPackageRepository;
import com.studioos.ai.repository.AiStudioSettingRepository;
import com.studioos.model.Branch;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin CRUD over all branch knowledge. Chat retrieval always reads through here, so edits take effect immediately. */
@Service @Transactional
public class AiKnowledgeService {
    private final AiBranchContext ctx;
    private final AiClassRepository classes;
    private final AiClassScheduleRepository schedules;
    private final AiPackageRepository packages;
    private final AiStudioSettingRepository settings;
    private final AiChatFaqRepository faqs;
    private final AiChatPolicyRepository policies;
    private final AiChatOfferRepository offers;

    public AiKnowledgeService(AiBranchContext ctx, AiClassRepository classes, AiClassScheduleRepository schedules,
        AiPackageRepository packages, AiStudioSettingRepository settings, AiChatFaqRepository faqs,
        AiChatPolicyRepository policies, AiChatOfferRepository offers) {
        this.ctx = ctx; this.classes = classes; this.schedules = schedules; this.packages = packages;
        this.settings = settings; this.faqs = faqs; this.policies = policies; this.offers = offers;
    }

    // ---- classes ----
    @Transactional(readOnly = true)
    public List<AiClassDto> listClasses(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        return classes.findByTenantIdAndBranchIdOrderByNameAsc(b.getTenantId(), b.getId()).stream().map(this::toClassDto).toList();
    }

    public AiClassDto saveClass(String tenant, AiClassDto dto) {
        Branch b = ctx.resolve(tenant, String.valueOf(dto.branchId()));
        AiClass entity = dto.id() == null ? new AiClass()
            : classes.findByIdAndTenantIdAndBranchId(dto.id(), b.getTenantId(), b.getId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found: " + dto.id()));
        entity.setTenantId(b.getTenantId()); entity.setBranchId(b.getId());
        entity.setName(dto.name().trim()); entity.setCategory(norm(dto.category()));
        entity.setMinAge(dto.minAge()); entity.setMaxAge(dto.maxAge());
        entity.setExperienceLevel(norm(dto.experienceLevel())); entity.setDescription(dto.description());
        if (dto.active() != null) entity.setActive(dto.active());
        return toClassDto(classes.save(entity));
    }

    public void deleteClass(String tenant, String branch, Long id) {
        Branch b = ctx.resolve(tenant, branch);
        AiClass entity = classes.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Class not found: " + id));
        entity.setActive(false); classes.save(entity);
    }

    // ---- schedules ----
    @Transactional(readOnly = true)
    public List<AiClassScheduleDto> listSchedules(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        return schedules.findByTenantIdAndBranchIdOrderByDayOfWeekAscStartTimeAsc(b.getTenantId(), b.getId()).stream().map(this::toScheduleDto).toList();
    }

    public AiClassScheduleDto saveSchedule(String tenant, AiClassScheduleDto dto) {
        Branch b = ctx.resolve(tenant, String.valueOf(dto.branchId()));
        classes.findByIdAndTenantIdAndBranchId(dto.aiClassId(), b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Class not found: " + dto.aiClassId()));
        AiClassSchedule entity = dto.id() == null ? new AiClassSchedule()
            : schedules.findByIdAndTenantIdAndBranchId(dto.id(), b.getTenantId(), b.getId())
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + dto.id()));
        entity.setTenantId(b.getTenantId()); entity.setBranchId(b.getId()); entity.setAiClassId(dto.aiClassId());
        entity.setDayOfWeek(dto.dayOfWeek().trim()); entity.setStartTime(dto.startTime().trim()); entity.setEndTime(dto.endTime().trim());
        entity.setBatchLabel(dto.batchLabel()); entity.setInstructorName(dto.instructorName());
        if (dto.active() != null) entity.setActive(dto.active());
        return toScheduleDto(schedules.save(entity));
    }

    public void deleteSchedule(String tenant, String branch, Long id) {
        Branch b = ctx.resolve(tenant, branch);
        AiClassSchedule entity = schedules.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + id));
        entity.setActive(false); schedules.save(entity);
    }

    // ---- packages ----
    @Transactional(readOnly = true)
    public List<AiPackageDto> listPackages(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        return packages.findByTenantIdAndBranchIdOrderByFeeAmountAsc(b.getTenantId(), b.getId()).stream().map(this::toPackageDto).toList();
    }

    public AiPackageDto savePackage(String tenant, AiPackageDto dto) {
        Branch b = ctx.resolve(tenant, String.valueOf(dto.branchId()));
        AiPackage entity = dto.id() == null ? new AiPackage()
            : packages.findByIdAndTenantIdAndBranchId(dto.id(), b.getTenantId(), b.getId())
                .orElseThrow(() -> new EntityNotFoundException("Package not found: " + dto.id()));
        entity.setTenantId(b.getTenantId()); entity.setBranchId(b.getId());
        entity.setName(dto.name().trim()); entity.setDurationMonths(dto.durationMonths());
        entity.setFeeAmount(dto.feeAmount()); entity.setAdmissionFee(dto.admissionFee());
        entity.setDescription(dto.description());
        if (dto.active() != null) entity.setActive(dto.active());
        return toPackageDto(packages.save(entity));
    }

    public void deletePackage(String tenant, String branch, Long id) {
        Branch b = ctx.resolve(tenant, branch);
        AiPackage entity = packages.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Package not found: " + id));
        entity.setActive(false); packages.save(entity);
    }

    // ---- settings ----
    @Transactional(readOnly = true)
    public List<AiStudioSettingDto> listSettings(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        return settings.findByTenantIdAndBranchIdOrderBySettingKeyAsc(b.getTenantId(), b.getId()).stream().map(this::toSettingDto).toList();
    }

    public AiStudioSettingDto saveSetting(String tenant, AiStudioSettingDto dto) {
        Branch b = ctx.resolve(tenant, String.valueOf(dto.branchId()));
        AiStudioSetting entity = settings.findByTenantIdAndBranchIdAndSettingKey(b.getTenantId(), b.getId(), dto.settingKey().trim())
            .orElseGet(AiStudioSetting::new);
        entity.setTenantId(b.getTenantId()); entity.setBranchId(b.getId());
        entity.setSettingKey(dto.settingKey().trim()); entity.setSettingValue(dto.settingValue());
        return toSettingDto(settings.save(entity));
    }

    public void deleteSetting(String tenant, String branch, Long id) {
        Branch b = ctx.resolve(tenant, branch);
        AiStudioSetting entity = settings.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Setting not found: " + id));
        settings.delete(entity);
    }

    // ---- faqs ----
    @Transactional(readOnly = true)
    public List<AiChatFaqDto> listFaqs(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        return faqs.findByTenantIdAndBranchIdOrderBySortOrderAscIdAsc(b.getTenantId(), b.getId()).stream().map(this::toFaqDto).toList();
    }

    public AiChatFaqDto saveFaq(String tenant, AiChatFaqDto dto) {
        Branch b = ctx.resolve(tenant, String.valueOf(dto.branchId()));
        AiChatFaq entity = dto.id() == null ? new AiChatFaq()
            : faqs.findByIdAndTenantIdAndBranchId(dto.id(), b.getTenantId(), b.getId())
                .orElseThrow(() -> new EntityNotFoundException("FAQ not found: " + dto.id()));
        entity.setTenantId(b.getTenantId()); entity.setBranchId(b.getId());
        entity.setQuestion(dto.question().trim()); entity.setAnswer(dto.answer().trim());
        entity.setKeywords(dto.keywords());
        if (dto.sortOrder() != null) entity.setSortOrder(dto.sortOrder());
        if (dto.active() != null) entity.setActive(dto.active());
        return toFaqDto(faqs.save(entity));
    }

    public void deleteFaq(String tenant, String branch, Long id) {
        Branch b = ctx.resolve(tenant, branch);
        AiChatFaq entity = faqs.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("FAQ not found: " + id));
        entity.setActive(false); faqs.save(entity);
    }

    // ---- policies ----
    @Transactional(readOnly = true)
    public List<AiChatPolicyDto> listPolicies(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        return policies.findByTenantIdAndBranchIdOrderByTitleAsc(b.getTenantId(), b.getId()).stream().map(this::toPolicyDto).toList();
    }

    public AiChatPolicyDto savePolicy(String tenant, AiChatPolicyDto dto) {
        Branch b = ctx.resolve(tenant, String.valueOf(dto.branchId()));
        AiChatPolicy entity = dto.id() == null ? new AiChatPolicy()
            : policies.findByIdAndTenantIdAndBranchId(dto.id(), b.getTenantId(), b.getId())
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + dto.id()));
        entity.setTenantId(b.getTenantId()); entity.setBranchId(b.getId());
        entity.setTitle(dto.title().trim()); entity.setBody(dto.body().trim());
        entity.setCategory(norm(dto.category()));
        if (dto.active() != null) entity.setActive(dto.active());
        return toPolicyDto(policies.save(entity));
    }

    public void deletePolicy(String tenant, String branch, Long id) {
        Branch b = ctx.resolve(tenant, branch);
        AiChatPolicy entity = policies.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + id));
        entity.setActive(false); policies.save(entity);
    }

    // ---- offers ----
    @Transactional(readOnly = true)
    public List<AiChatOfferDto> listOffers(String tenant, String branch) {
        Branch b = ctx.resolve(tenant, branch);
        return offers.findByTenantIdAndBranchIdOrderByValidUntilAsc(b.getTenantId(), b.getId()).stream().map(this::toOfferDto).toList();
    }

    public AiChatOfferDto saveOffer(String tenant, AiChatOfferDto dto) {
        Branch b = ctx.resolve(tenant, String.valueOf(dto.branchId()));
        AiChatOffer entity = dto.id() == null ? new AiChatOffer()
            : offers.findByIdAndTenantIdAndBranchId(dto.id(), b.getTenantId(), b.getId())
                .orElseThrow(() -> new EntityNotFoundException("Offer not found: " + dto.id()));
        entity.setTenantId(b.getTenantId()); entity.setBranchId(b.getId());
        entity.setTitle(dto.title().trim()); entity.setBody(dto.body().trim());
        entity.setValidFrom(dto.validFrom()); entity.setValidUntil(dto.validUntil());
        if (dto.active() != null) entity.setActive(dto.active());
        return toOfferDto(offers.save(entity));
    }

    public void deleteOffer(String tenant, String branch, Long id) {
        Branch b = ctx.resolve(tenant, branch);
        AiChatOffer entity = offers.findByIdAndTenantIdAndBranchId(id, b.getTenantId(), b.getId())
            .orElseThrow(() -> new EntityNotFoundException("Offer not found: " + id));
        entity.setActive(false); offers.save(entity);
    }

    private String norm(String value) { return value == null || value.isBlank() ? null : value.trim().toUpperCase(); }

    private AiClassDto toClassDto(AiClass c) {
        return new AiClassDto(c.getId(), c.getTenantId(), c.getBranchId(), c.getName(), c.getCategory(),
            c.getMinAge(), c.getMaxAge(), c.getExperienceLevel(), c.getDescription(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private AiClassScheduleDto toScheduleDto(AiClassSchedule s) {
        return new AiClassScheduleDto(s.getId(), s.getTenantId(), s.getBranchId(), s.getAiClassId(), s.getDayOfWeek(),
            s.getStartTime(), s.getEndTime(), s.getBatchLabel(), s.getInstructorName(), s.isActive());
    }

    private AiPackageDto toPackageDto(AiPackage p) {
        return new AiPackageDto(p.getId(), p.getTenantId(), p.getBranchId(), p.getName(), p.getDurationMonths(),
            p.getFeeAmount(), p.getAdmissionFee(), p.getDescription(), p.isActive());
    }

    private AiStudioSettingDto toSettingDto(AiStudioSetting s) {
        return new AiStudioSettingDto(s.getId(), s.getTenantId(), s.getBranchId(), s.getSettingKey(), s.getSettingValue());
    }

    private AiChatFaqDto toFaqDto(AiChatFaq f) {
        return new AiChatFaqDto(f.getId(), f.getTenantId(), f.getBranchId(), f.getQuestion(), f.getAnswer(),
            f.getKeywords(), f.getSortOrder(), f.isActive());
    }

    private AiChatPolicyDto toPolicyDto(AiChatPolicy p) {
        return new AiChatPolicyDto(p.getId(), p.getTenantId(), p.getBranchId(), p.getTitle(), p.getBody(), p.getCategory(), p.isActive());
    }

    private AiChatOfferDto toOfferDto(AiChatOffer o) {
        return new AiChatOfferDto(o.getId(), o.getTenantId(), o.getBranchId(), o.getTitle(), o.getBody(), o.getValidFrom(), o.getValidUntil(), o.isActive());
    }
}
