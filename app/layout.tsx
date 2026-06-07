import type { Metadata } from "next";
import "./globals.css";
import { FeedbackChat } from '@automate/feedback-lib/FeedbackChat';

export const metadata: Metadata = {
  title: "picaStats",
  description: "picawish & multi-site visitor analytics — hourly stats updates and notifications",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}
        <FeedbackChat issuesPath="/feedback-lib-issues" />
</body>
    </html>
  );
}
