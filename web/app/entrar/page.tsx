"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/components/auth/AuthProvider";
import { ApiError } from "@/lib/api";
import { Koda } from "@/components/ui/Koda";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      router.push("/aprender");
    } catch (e) {
      // Mensagem generica de proposito: o backend ja unifica "e-mail
      // inexistente" e "senha errada" pra nao permitir enumerar contas (SEC-06).
      setError(e instanceof ApiError ? e.message : "Nao foi possivel entrar.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen grid place-items-center bg-[var(--bg-canvas)] px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center gap-2 mb-6">
          <Koda state="idle" size={56} />
          <h1 className="text-2xl font-extrabold text-[var(--accent)]">KODA</h1>
        </div>

        <Card>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input
              label="E-mail"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <Input
              label="Senha"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />

            {error && (
              <p className="text-sm font-semibold text-[var(--error)]" role="alert">
                {error}
              </p>
            )}

            <Button type="submit" fullWidth loading={submitting}>
              Entrar
            </Button>
          </form>
        </Card>

        <p className="mt-4 text-center text-sm text-[var(--text-secondary)]">
          Ainda nao tem conta?{" "}
          <Link href="/registrar" className="font-extrabold text-[var(--accent)]">
            Cadastre-se
          </Link>
        </p>
      </div>
    </div>
  );
}
