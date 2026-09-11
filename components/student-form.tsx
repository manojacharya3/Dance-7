"use client";

import { useState } from "react";
import type { Student, StudentPayload } from "@/lib/students";
import type { Branch } from "@/lib/branches";

const emptyStudent: StudentPayload = {
  tenantId: "default",
  branchId: 1,
  firstName: "",
  lastName: "",
  dateOfBirth: "",
  gender: "",
  email: "",
  phone: "",
  address: "",
  emergencyContact: "",
  parentName: "",
  parentPhone: "",
  danceStyle: "",
  skillLevel: "",
  medicalNotes: "",
  studentPhotoUrl: "",
  active: true,
};

type StudentFormProps = {
  initialStudent?: Student;
  submitLabel: string;
  onSubmit: (student: StudentPayload) => Promise<void>;
  branches?: Branch[];
};

export function StudentForm({ initialStudent, submitLabel, onSubmit, branches = [] }: StudentFormProps) {
  const [form, setForm] = useState<StudentPayload>(initialStudent ? toPayload(initialStudent) : emptyStudent);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  function update(field: keyof StudentPayload, value: string | number | boolean) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await onSubmit(form);
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : "Unable to save student.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-8">
      <section className="rounded-xl border border-slate-200/80 bg-white p-6">
        <div className="mb-6"><p className="text-xs font-bold uppercase tracking-[0.16em] text-violet-700">Profile</p><h2 className="mt-1 text-lg font-semibold text-slate-900">Student information</h2></div>
        <div className="grid gap-5 sm:grid-cols-2">
                    <label className="block text-sm font-medium text-slate-700">Branch<select required value={form.branchId} onChange={(event) => update("branchId", Number(event.target.value))} className="mt-2 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm"><option value={0} disabled>Select branch</option>{branches.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select></label>
          <Field label="First name" required value={form.firstName} onChange={(value) => update("firstName", value)} />
          <Field label="Last name" required value={form.lastName} onChange={(value) => update("lastName", value)} />
          <Field label="Date of birth" type="date" value={form.dateOfBirth ?? ""} onChange={(value) => update("dateOfBirth", value)} />
          <SelectField label="Gender" value={form.gender ?? ""} onChange={(value) => update("gender", value)} options={["Female", "Male", "Non-binary", "Prefer not to say"]} />
          <SelectField label="Dance style" value={form.danceStyle ?? ""} onChange={(value) => update("danceStyle", value)} options={["Ballet", "Jazz", "Hip-hop", "Contemporary", "Tap", "Acro"]} />
          <SelectField label="Skill level" value={form.skillLevel ?? ""} onChange={(value) => update("skillLevel", value)} options={["Beginner", "Intermediate", "Advanced", "Professional"]} />
        </div>
      </section>
      <section className="rounded-xl border border-slate-200/80 bg-white p-6">
        <div className="mb-6"><p className="text-xs font-bold uppercase tracking-[0.16em] text-violet-700">Contact</p><h2 className="mt-1 text-lg font-semibold text-slate-900">Contact details</h2></div>
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Email" type="email" value={form.email ?? ""} onChange={(value) => update("email", value)} />
          <Field label="Phone" value={form.phone ?? ""} onChange={(value) => update("phone", value)} />
          <Field label="Parent / guardian" value={form.parentName ?? ""} onChange={(value) => update("parentName", value)} />
          <Field label="Parent phone" value={form.parentPhone ?? ""} onChange={(value) => update("parentPhone", value)} />
          <Field label="Emergency contact" value={form.emergencyContact ?? ""} onChange={(value) => update("emergencyContact", value)} />
          <Field label="Photo URL" value={form.studentPhotoUrl ?? ""} onChange={(value) => update("studentPhotoUrl", value)} />
          <div className="sm:col-span-2"><Field label="Address" value={form.address ?? ""} onChange={(value) => update("address", value)} /></div>
        </div>
      </section>
      <section className="rounded-xl border border-slate-200/80 bg-white p-6">
        <div className="mb-6"><p className="text-xs font-bold uppercase tracking-[0.16em] text-violet-700">Notes</p><h2 className="mt-1 text-lg font-semibold text-slate-900">Care information</h2></div>
        <label className="block text-sm font-medium text-slate-700">Medical notes<textarea value={form.medicalNotes ?? ""} onChange={(event) => update("medicalNotes", event.target.value)} rows={4} className="mt-2 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100" /></label>
      </section>
      {error && <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      <div className="flex justify-end"><button type="submit" disabled={saving} className="rounded-lg bg-violet-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#2c3d46] disabled:cursor-not-allowed disabled:opacity-60">{saving ? "Saving..." : submitLabel}</button></div>
    </form>
  );
}

function Field({ label, value, onChange, type = "text", required = false }: { label: string; value: string; onChange: (value: string) => void; type?: string; required?: boolean }) {
  return <label className="block text-sm font-medium text-slate-700">{label}{required && <span className="ml-1 text-[#d45d4d">*</span>}<input required={required} type={type} value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100" /></label>;
}

function SelectField({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[] }) {
  return <label className="block text-sm font-medium text-slate-700">{label}<select value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100"><option value="">Select...</option>{options.map((option) => <option key={option} value={option}>{option}</option>)}</select></label>;
}

function toPayload(student: Student): StudentPayload {
  const { id, createdAt, updatedAt, ...payload } = student;
  return payload;
}
