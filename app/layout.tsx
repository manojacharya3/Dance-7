import type { Metadata } from "next";
import { AuthGuard } from "@/components/auth-guard";
import "./globals.css";

export const metadata: Metadata = {
  title: "Dance7 — Studio Management",
  description: "Modern studio management for Dance7 - The Art Factory. Students, batches, payments, attendance and reminders.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <AuthGuard>{children}</AuthGuard>
      </body>
    </html>
  );
}
