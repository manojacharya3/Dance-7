import * as React from "react";
import { cn } from "@/lib/utils";

type Variant = "primary" | "dark" | "secondary" | "ghost" | "danger";

const styles: Record<Variant, string> = {
  primary: "d7-btn-primary",
  dark: "d7-btn-dark",
  secondary: "d7-btn-secondary",
  ghost: "d7-btn-ghost",
  danger: "d7-btn-danger",
};

export function Button({
  variant = "primary",
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return <button className={cn(styles[variant], className)} {...props} />;
}
