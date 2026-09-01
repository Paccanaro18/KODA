"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth/AuthProvider";
import { Koda } from "@/components/ui/Koda";
import type { ReactNode } from "react";

/** Envolve uma tela do produto e manda pro login quem nao tem sessao. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/entrar");
    }
  }, [status, router]);

  if (status !== "authenticated") {
    return (
      <div className="min-h-screen grid place-items-center bg-[var(--bg-canvas)]">
        <Koda state="thinking" size={64} />
      </div>
    );
  }

  return <>{children}</>;
}
