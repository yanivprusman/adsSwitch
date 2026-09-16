import type { Metadata } from "next";
import "./globals.css";
import { FeedbackChat } from '@automate/feedback-lib/FeedbackChat';

export const metadata: Metadata = {
  title: "adsSwitch",
  description: "מודעות — one tap switches Google Ads between the nationwide set and the local set (Be'er Sheva + Midreshet Ben-Gurion), or turns them all off",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>{children}
        <FeedbackChat issuesPath="/feedback-lib-issues" />
</body>
    </html>
  );
}
