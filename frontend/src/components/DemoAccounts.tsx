import { useQuery } from '@tanstack/react-query'
import { FlaskConical } from 'lucide-react'
import { demoApi } from '../api/client'
import { ROLE_LABELS } from '../api/types'
import type { Role } from '../api/types'

/**
 * Site de demonstration : comptes d'essai de cette page (l'equipe sur l'espace equipe, le client sur la connexion client).
 * Un clic remplit le formulaire. Rien ne s'affiche sur le vrai site.
 */
export function DemoAccounts({ roles, onPick }: { roles: Role[]; onPick: (email: string, password: string) => void }) {
  const { data } = useQuery({ queryKey: ['demo'], queryFn: demoApi.info, staleTime: Infinity })
  const accounts = data?.enabled ? data.accounts.filter((a) => roles.includes(a.role)) : []
  if (!data?.password || accounts.length === 0) return null
  return (
    <section className="mt-6 rounded-2xl bg-white p-4 text-sm ring-1 ring-stone-200" aria-label="Comptes de démonstration">
      <p className="flex items-center gap-2 font-semibold text-slate-700">
        <FlaskConical className="size-4 text-brand-700" aria-hidden />
        Site de démonstration : essayez avec un compte d’essai
      </p>
      <div className="mt-3 grid gap-2">
        {accounts.map((a) => (
          <button
            key={a.email}
            type="button"
            onClick={() => onPick(a.email, data.password!)}
            className="flex items-center justify-between gap-3 rounded-xl px-3 py-2 text-left ring-1 ring-stone-200 hover:bg-brand-50 hover:ring-brand-200"
          >
            <span>
              <span className="font-semibold">{a.name}</span>
              <span className="block text-xs text-slate-500">{a.email}</span>
            </span>
            <span className="shrink-0 rounded-full bg-brand-50 px-2 py-0.5 text-xs font-semibold text-brand-800">{ROLE_LABELS[a.role]}</span>
          </button>
        ))}
      </div>
      <p className="mt-2 text-xs text-slate-500">Données fictives : les emails ne sont jamais envoyés et les paiements sont simulés.</p>
    </section>
  )
}
