"use client";

import { Search } from "lucide-react";
import type { Batch } from "@/lib/batches";
import type { Branch } from "@/lib/branches";

export function ManagementFilters({
  branches,
  batches,
  branchId,
  batchId,
  search,
  onBranchChange,
  onBatchChange,
  onSearch,
  searchPlaceholder = "Search",
}: {
  branches: Branch[];
  batches: Batch[];
  branchId: number;
  batchId: number;
  search: string;
  onBranchChange: (id: number) => void;
  onBatchChange: (id: number) => void;
  onSearch: (value: string) => void;
  searchPlaceholder?: string;
}) {
  const visibleBatches = batches.filter((batch) => !branchId || batch.branchId === branchId);
  return (
    <div className="d7-filterbar">
      <label className="relative flex-1">
        <Search size={16} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[#8a8a8a]" />
        <input
          value={search}
          onChange={(event) => onSearch(event.target.value)}
          placeholder={searchPlaceholder}
          aria-label={searchPlaceholder}
          className="d7-input pl-10"
        />
      </label>
      <div className="grid grid-cols-2 gap-3 lg:flex">
        <select
          value={branchId}
          aria-label="Filter by branch"
          onChange={(event) => {
            onBranchChange(Number(event.target.value));
            onBatchChange(0);
          }}
          className="d7-input lg:w-44"
        >
          <option value={0}>All branches</option>
          {branches.map((branch) => (
            <option key={branch.id} value={branch.id}>
              {branch.name}
            </option>
          ))}
        </select>
        <select
          value={batchId}
          aria-label="Filter by batch"
          onChange={(event) => onBatchChange(Number(event.target.value))}
          className="d7-input lg:w-52"
        >
          <option value={0}>All batches</option>
          {visibleBatches.map((batch) => (
            <option key={batch.id} value={batch.id}>
              {branches.find((branch) => branch.id === batch.branchId)?.name || "Branch"} | {batch.batchName}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
