import type { Metadata } from "next";
import { AuthGuard } from "@/components/auth-guard";
import { AiChatWidget } from "@/components/ai-chat-widget";
import "./globals.css";

export const metadata: Metadata = {
  title: "Dance7 — The Art Factory",
  description: "Dance7 The Art Factory: premium studio management. Students, batches, payments, attendance and reminders.",
  icons: { icon: "/brand/dance7-logo.jpg", apple: "/brand/dance7-logo.jpg" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <AuthGuard>{children}</AuthGuard>
        <AiChatWidget />
      </body>
    </html>
  );
}
