"use client";

import { useEffect, useState } from "react";
import { Pencil, Plus, Shield, Trash2, UserX } from "lucide-react";
import { Navbar } from "@/components/navbar";
import { Sidebar } from "@/components/sidebar";
import { getBranches, type Branch } from "@/lib/branches";
import { currentUser, type AuthUser } from "@/lib/auth";
import { getInstructors, type Instructor } from "@/lib/instructors";
import {
  createManagedUser,
  disableManagedUser,
  getManagedUsers,
  removeManagedUser,
  type ManagedUser,
  updateManagedUser,
} from "@/lib/admin-users";

const roles = ["OWNER", "BRANCH_HEAD", "INSTRUCTOR", "DEVELOPER"];

type StatusFilter = "ACTIVE" | "DISABLED" | "ALL";

export default function UsersAdminPage() {
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [instructors, setInstructors] = useState<Instructor[]>([]);
  const [editing, setEditing] = useState<ManagedUser | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [confirmUser, setConfirmUser] = useState<ManagedUser | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [removeUser, setRemoveUser] = useState<ManagedUser | null>(null);
  const [removing, setRemoving] = useState(false);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ACTIVE");
  const [me, setMe] = useState<AuthUser | null>(null);

  const readOnly = (me?.roles ?? [])
    .map((role) => role.toUpperCase().replace(/^ROLE_/, ""))
    .includes("DEVELOPER");

  const load = () =>
    Promise.all([getManagedUsers(), getBranches(), getInstructors()])
      .then(([userList, branchList, instructorPage]) => {
        setUsers(userList);
        setBranches(branchList);
        setInstructors(instructorPage.content);
      })
      .catch((e) =>
        setError(e instanceof Error ? e.message : "Unable to load users.")
      );

  useEffect(() => {
    load();
    currentUser().then(setMe).catch(() => undefined);
  }, []);

  const visibleUsers = users.filter((user) => {
    if (statusFilter === "ALL") return true;
    if (statusFilter === "DISABLED") return !user.enabled;
    return user.enabled;
  });

  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const role = String(form.get("role"));
    const payload: ManagedUser = {
      id: editing?.id || 0,
      tenantId: "default",
      fullName: String(form.get("fullName")),
      email: String(form.get("email")),
      password: String(form.get("password") || ""),
      role,
      branchId: Number(form.get("branchId")) || undefined,
      instructorId: Number(form.get("instructorId")) || undefined,
      enabled: true,
    };
    try {
      if (editing) await updateManagedUser(editing.id, payload);
      else await createManagedUser(payload);
      setShowForm(false);
      setEditing(null);
      setError("");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to save user.");
    }
  }

  function requestDelete(user: ManagedUser) {
    setConfirmUser(user);
    setError("");
    setSuccess("");
  }

  function cancelDelete() {
    if (deleting) return;
    setConfirmUser(null);
  }

  async function confirmDelete() {
    if (!confirmUser) return;
    const target = confirmUser;
    setDeleting(true);
    setError("");
    setSuccess("");
    try {
      await disableManagedUser(target.id);
      // Remove immediately so no manual refresh is needed.
      setUsers((current) => current.filter((user) => user.id !== target.id));
      setSuccess("User disabled successfully");
      setConfirmUser(null);
      // Refresh the list automatically to stay in sync with the server.
      await load();
    } catch {
      setError("Unable to delete user. Please try again.");
      // Keep the row in the table on failure; do not clear confirmUser
      // so the user can retry or cancel.
    } finally {
      setDeleting(false);
    }
  }

  function requestRemove(user: ManagedUser) {
    if (user.enabled) return;
    setRemoveUser(user);
    setError("");
    setSuccess("");
  }

  function cancelRemove() {
    if (removing) return;
    setRemoveUser(null);
  }

  async function confirmRemove() {
    if (!removeUser) return;
    const target = removeUser;
    setRemoving(true);
    setError("");
    setSuccess("");
    try {
      await removeManagedUser(target.id);
      setUsers((current) => current.filter((user) => user.id !== target.id));
      setSuccess("User removed successfully");
      setRemoveUser(null);
      await load();
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Unable to remove user. Please try again."
      );
    } finally {
      setRemoving(false);
    }
  }

  return (
    <div className="min-h-screen">
      <Sidebar />
      <div className="lg:pl-[272px]">
        <Navbar />
        <main className="d7-page">
          <div className="mb-8 flex items-end justify-between">
            <div>
              <p className="mb-2 text-sm font-semibold text-[#ff6b6b]">
                Administration
              </p>
              <h1 className="text-3xl font-semibold">Users</h1>
              <p className="mt-2 text-sm text-[#b3b3b3]">
                Manage user access, roles, branches, and instructor links.
              </p>
            </div>
            {!readOnly && (
              <button
                onClick={() => {
                  setEditing(null);
                  setShowForm(true);
                }}
                className="flex items-center gap-2 rounded-lg bg-[#ff1a1a] px-4 py-2.5 text-sm font-semibold text-white"
              >
                <Plus size={17} />
                Create user
              </button>
            )}
          </div>
          {error && (
            <p className="mb-6 rounded-lg bg-[#ef4444]/10 p-4 text-sm text-[#ff9999]">
              {error}
            </p>
          )}
          {success && (
            <p className="mb-6 rounded-lg bg-[#eaf7f0] p-4 text-sm text-[#1a7a4c]">
              {success}
            </p>
          )}
          <div className="mb-4 flex items-center gap-3">
            <label
              htmlFor="user-status-filter"
              className="text-sm font-medium text-[#e5e5e5]"
            >
              Status
            </label>
            <select
              id="user-status-filter"
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(event.target.value as StatusFilter)
              }
              className="rounded-lg border px-3 py-2.5 text-sm"
            >
              <option value="ACTIVE">Active</option>
              <option value="DISABLED">Disabled</option>
              <option value="ALL">All</option>
            </select>
          </div>
          <div className="overflow-x-auto rounded-xl border border-[#2a2a2a] bg-[#111111]">
            <table className="w-full min-w-[850px] text-left">
              <thead className="bg-[#161616] text-xs uppercase tracking-[0.12em] text-[#8a8a8a]">
                <tr>
                  <th className="px-5 py-3">Name</th>
                  <th className="px-5 py-3">Email</th>
                  <th className="px-5 py-3">Role</th>
                  <th className="px-5 py-3">Branch</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleUsers.map((user) => (
                  <tr key={user.id} className="border-t border-[#222222]">
                    <td className="px-5 py-4 font-semibold">{user.fullName}</td>
                    <td className="px-5 py-4 text-sm">{user.email}</td>
                    <td className="px-5 py-4">
                      <span className="inline-flex items-center gap-1 text-sm">
                        <Shield size={14} className="text-[#ff6b6b]" />
                        {user.role}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-sm">
                      {branches.find((branch) => branch.id === user.branchId)
                        ?.name || "All branches"}
                    </td>
                    <td className="px-5 py-4 text-sm">
                      {user.enabled ? "Active" : "Disabled"}
                    </td>
                    <td className="px-5 py-4">
                      {readOnly ? (
                        <span className="text-sm text-[#8a8a8a]">—</span>
                      ) : (
                        <div className="flex gap-3">
                          <button
                            onClick={() => {
                              setEditing(user);
                              setShowForm(true);
                            }}
                            aria-label="Edit user"
                            className="text-[#ff6b6b]"
                          >
                            <Pencil size={16} />
                          </button>
                        {user.enabled ? (
                          <button
                            onClick={() => requestDelete(user)}
                            aria-label="Disable user"
                            className="text-[#ff9999]"
                          >
                            <UserX size={16} />
                          </button>
                        ) : (
                          <button
                            onClick={() => requestRemove(user)}
                            aria-label="Remove user"
                            className="text-[#ff9999]"
                          >
                            <Trash2 size={16} />
                          </button>
                        )}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!visibleUsers.length && (
              <p className="p-8 text-center text-sm text-[#b3b3b3]">
                No users match this filter.
              </p>
            )}
          </div>
          {showForm && (
            <div className="fixed inset-0 z-10 flex items-center justify-center bg-[#ff1a1a]/40 p-6">
              <form
                onSubmit={save}
                className="w-full max-w-lg space-y-4 rounded-2xl bg-[#111111] p-6"
              >
                <h2 className="text-xl font-semibold">
                  {editing ? "Edit user" : "Create user"}
                </h2>
                <input
                  name="fullName"
                  required
                  defaultValue={editing?.fullName || ""}
                  placeholder="Full name"
                  className="w-full rounded-lg border px-3 py-2.5"
                />
                <input
                  name="email"
                  required
                  type="email"
                  defaultValue={editing?.email || ""}
                  placeholder="Email"
                  className="w-full rounded-lg border px-3 py-2.5"
                />
                <input
                  name="password"
                  type="password"
                  required={!editing}
                  placeholder={
                    editing ? "New password (optional)" : "Password"
                  }
                  className="w-full rounded-lg border px-3 py-2.5"
                />
                <select
                  name="role"
                  defaultValue={editing?.role || "INSTRUCTOR"}
                  className="w-full rounded-lg border px-3 py-2.5"
                >
                  <option value="OWNER">Owner</option>
                  <option value="BRANCH_HEAD">Branch head</option>
                  <option value="INSTRUCTOR">Instructor</option>
                  <option value="DEVELOPER">Developer</option>
                </select>
                <select
                  name="branchId"
                  defaultValue={editing?.branchId || ""}
                  className="w-full rounded-lg border px-3 py-2.5"
                >
                  <option value="">All branches</option>
                  {branches.map((branch) => (
                    <option key={branch.id} value={branch.id}>
                      {branch.name}
                    </option>
                  ))}
                </select>
                <select
                  name="instructorId"
                  defaultValue={editing?.instructorId || ""}
                  className="w-full rounded-lg border px-3 py-2.5"
                >
                  <option value="">Instructor link (optional)</option>
                  {instructors.map((instructor) => (
                    <option key={instructor.id} value={instructor.id}>
                      {instructor.firstName} {instructor.lastName}
                    </option>
                  ))}
                </select>
                <div className="flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setShowForm(false);
                      setEditing(null);
                    }}
                    className="rounded-lg border px-4 py-2.5 text-sm"
                  >
                    Cancel
                  </button>
                  <button className="rounded-lg bg-[#ff1a1a] px-4 py-2.5 text-sm font-semibold text-white">
                    Save user
                  </button>
                </div>
              </form>
            </div>
          )}
          {confirmUser && (
            <div
              className="fixed inset-0 z-10 flex items-center justify-center bg-[#ff1a1a]/40 p-6"
              role="dialog"
              aria-modal="true"
              aria-labelledby="confirm-user-deletion-title"
            >
              <div className="w-full max-w-md rounded-2xl bg-[#111111] p-6">
                <h2
                  id="confirm-user-deletion-title"
                  className="text-xl font-semibold"
                >
                  Confirm User Deletion
                </h2>
                <p className="mt-3 text-sm text-[#e5e5e5]">
                  Are you sure you want to delete/disable this user?
                </p>
                <div className="mt-4 rounded-lg bg-[#161616] p-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8a8a8a]">
                    User
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    {confirmUser.fullName}
                  </p>
                  <p className="text-sm text-[#e5e5e5]">{confirmUser.email}</p>
                </div>
                <div className="mt-6 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={cancelDelete}
                    disabled={deleting}
                    className="rounded-lg border px-4 py-2.5 text-sm disabled:opacity-40"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={confirmDelete}
                    disabled={deleting}
                    className="rounded-lg bg-red-600 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
                  >
                    {deleting ? "Deleting..." : "Delete User"}
                  </button>
                </div>
              </div>
            </div>
          )}
          {removeUser && (
            <div
              className="fixed inset-0 z-10 flex items-center justify-center bg-[#ff1a1a]/40 p-6"
              role="dialog"
              aria-modal="true"
              aria-labelledby="permanently-remove-user-title"
            >
              <div className="w-full max-w-md rounded-2xl bg-[#111111] p-6">
                <h2
                  id="permanently-remove-user-title"
                  className="text-xl font-semibold"
                >
                  Permanently Remove User
                </h2>
                <p className="mt-3 text-sm text-[#e5e5e5]">
                  This action cannot be undone.
                </p>
                <div className="mt-4 rounded-lg bg-[#161616] p-4">
                  <p className="text-xs font-semibold uppercase tracking-[0.12em] text-[#8a8a8a]">
                    User
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    {removeUser.fullName}
                  </p>
                  <p className="text-sm text-[#e5e5e5]">{removeUser.email}</p>
                </div>
                <div className="mt-6 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={cancelRemove}
                    disabled={removing}
                    className="rounded-lg border px-4 py-2.5 text-sm disabled:opacity-40"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={confirmRemove}
                    disabled={removing}
                    className="rounded-lg bg-red-600 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
                  >
                    {removing ? "Removing..." : "Remove User"}
                  </button>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
