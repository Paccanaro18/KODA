const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export interface AuthTokens {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface Me {
  id: string;
  email: string;
  role: string;
  displayName: string;
  locale: string;
  timezone: string;
  learningGoal: string | null;
  dailyGoalMinutes: number;
  prefersReducedMotion: boolean;
}

/** `state` usa os mesmos nomes que SkillNode.tsx ja consome: nenhuma traducao aqui. */
export interface CurriculumConcept {
  id: string;
  title: string;
  state: "locked" | "available" | "active" | "completed" | "mastered" | "needsReview";
  progress: number;
}

export interface CurriculumTopic {
  id: string;
  name: string;
  description: string | null;
  concepts: CurriculumConcept[];
}

export interface CurriculumMap {
  topics: CurriculumTopic[];
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fields?: Record<string, string>,
  ) {
    super(message);
  }
}

/**
 * `credentials: "include"` em toda chamada: e o que faz o navegador enviar e
 * aceitar o cookie httpOnly do refresh token, mesmo com frontend e backend em
 * portas diferentes (SEC-07 / ADR-0002).
 */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });

  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new ApiError(
      res.status,
      body?.message ?? "Nao foi possivel completar a acao. Tente novamente.",
      body?.fields,
    );
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return res.json() as Promise<T>;
}

export function register(input: {
  email: string;
  password: string;
  displayName?: string;
}): Promise<AuthTokens> {
  return request("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function login(input: { email: string; password: string }): Promise<AuthTokens> {
  return request("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

/** So funciona se o cookie do refresh token ainda for valido. */
export function refresh(): Promise<AuthTokens> {
  return request("/api/v1/auth/refresh", { method: "POST" });
}

export function logout(): Promise<void> {
  return request("/api/v1/auth/logout", { method: "POST" });
}

export function me(accessToken: string): Promise<Me> {
  return request("/api/v1/auth/me", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

export function curriculumMap(accessToken: string): Promise<CurriculumMap> {
  return request("/api/v1/curriculum/map", {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}
