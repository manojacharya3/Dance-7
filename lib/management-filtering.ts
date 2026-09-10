import { getStudentBatches, type Batch } from "@/lib/batches";
import type { Student } from "@/lib/students";

export async function getStudentBatchMap(students: Student[]) {
  const entries = await Promise.all(students.map(async (student) => {
    try {
      const assignments = await getStudentBatches(student.id);
      return [student.id, assignments.map((assignment) => assignment.batchId)] as const;
    } catch {
      return [student.id, [] as number[]] as const;
    }
  }));
  return new Map(entries);
}

export function matchesBatch(batchId: number, studentId: number, studentBatchMap: Map<number, number[]>) {
  return !batchId || studentBatchMap.get(studentId)?.includes(batchId) === true;
}

export function visibleBatches(batches: Batch[], branchId: number) {
  return batches.filter((batch) => !branchId || batch.branchId === branchId);
}
