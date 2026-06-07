import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "picaStats",
  description:
    "picawish & multi-site visitor analytics — hourly stats updates and notifications",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
