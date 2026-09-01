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

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      await register(email, password, displayName || undefined);
      router.push("/aprender");
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message);
        setFieldErrors(e.fields ?? {});
      } else {
        setError("Nao foi possivel criar sua conta.");
      }
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
              label="Nome"
              autoComplete="name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
            <Input
              label="E-mail"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={fieldErrors.email}
              required
            />
            <Input
              label="Senha"
              type="password"
              autoComplete="new-password"
              minLength={12}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={fieldErrors.password}
              required
            />
            <p className="-mt-2 text-xs text-[var(--text-muted)]">
              Minimo de 12 caracteres.
            </p>

            {error && (
              <p className="text-sm font-semibold text-[var(--error)]" role="alert">
                {error}
              </p>
            )}

            <Button type="submit" fullWidth loading={submitting}>
              Criar conta
            </Button>
          </form>
        </Card>

        <p className="mt-4 text-center text-sm text-[var(--text-secondary)]">
          Ja tem conta?{" "}
          <Link href="/entrar" className="font-extrabold text-[var(--accent)]">
            Entrar
          </Link>
        </p>
      </div>
    </div>
  );
}
