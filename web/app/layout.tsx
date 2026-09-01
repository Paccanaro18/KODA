import type { Metadata } from "next";
import { Nunito, JetBrains_Mono } from "next/font/google";
import { ThemeProvider, themeInitScript } from "@/components/theme/ThemeProvider";
import { AuthProvider } from "@/components/auth/AuthProvider";
import "./globals.css";

// Fontes servidas pelo proprio dominio via next/font: sem request a terceiro
// em runtime, sem layout shift e sem depender de CDN externo.
// Nunito e a fonte de UI: arredondada, de contraste baixo, com o desenho de
// letra que faz o texto soar falado em vez de gerado. Vai ate o peso 900
// porque nesta linguagem visual titulo e rotulo de botao sao pesados.
const nunito = Nunito({
  subsets: ["latin"],
  weight: ["400", "600", "700", "800", "900"],
  variable: "--font-nunito",
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: "KODA",
  description:
    "Plataforma de aprendizagem adaptativa para tecnologia — pratique programacao, DevOps, cloud, redes e mais.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <head>
        {/* Antes da primeira pintura, para nao piscar o tema errado. */}
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className={`${nunito.variable} ${jetbrainsMono.variable} antialiased`}>
        <ThemeProvider>
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
