/**
 * Shared IST-friendly time formatter. Database and API values stay in 24-hour
 * form ("16:30:00"); only presentation uses this. en-IN locale, 12-hour clock.
 *
 * formatTime12Hour("16:30:00") -> "4:30 PM"
 * formatTime12Hour("09:00")    -> "9:00 AM"
 * formatTime12Hour("4:30 PM")  -> "4:30 PM" (already 12-hour, passed through)
 */
export function formatTime12Hour(value: string | null | undefined): string {
  if (value == null) return "—";
  const text = value.trim();
  if (!text) return "—";
  if (/[AP]\.?M\.?$/i.test(text)) return text.toUpperCase().replace(/\./g, "");
  const match = text.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/);
  if (!match) return text;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return text;
  const probe = new Date(2000, 0, 1, hours, minutes);
  return new Intl.DateTimeFormat("en-IN", { hour: "numeric", minute: "2-digit", hour12: true })
    .format(probe)
    .toUpperCase();
}

/** "16:30:00" + "17:30:00" -> "4:30 PM - 5:30 PM" */
export function formatTimeRange(start: string | null | undefined, end: string | null | undefined): string {
  return `${formatTime12Hour(start)} - ${formatTime12Hour(end)}`;
}
