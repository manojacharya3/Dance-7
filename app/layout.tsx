import type { Metadata } from "next";
import { AuthGuard } from "@/components/auth-guard";
import "./globals.css";

export const metadata: Metadata = {
  title: "Dance7 - The Art Factory",
  description: "Studio management for Dance7 - The Art Factory.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body><AuthGuard>{children}</AuthGuard></body>
    </html>
  );
}
