import { useQuery } from '@tanstack/react-query'
import { CalendarCheck, ClipboardList, FileText, Mail, ShieldCheck, Wallet } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Link } from 'react-router-dom'
import { dashboardApi, errorMessage } from '../../api/client'
import { ROLE_LABELS } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { Alert, Card, Loader, PageHeader } from '../../components/ui'
import { formatEuros, formatPeriod } from '../../lib/format'

function Tile({ label, value, detail, to, Icon }: { label: string; value: string; detail?: string; to: string; Icon: LucideIcon }) {
  return (
    <Link to={to} className="block rounded-2xl bg-white p-5 ring-1 ring-stone-200 transition hover:shadow-md">
      <p className="flex items-center gap-2 text-sm font-medium text-slate-500">
        <Icon className="size-4" aria-hidden />
        {label}
      </p>
      <p className="mt-2 text-3xl font-extrabold tabular-nums">{value}</p>
      {detail && <p className="mt-1 text-sm text-slate-500">{detail}</p>}
    </Link>
  )
}

// US-20 : tableau de bord de l'equipe (activite, encaissements, materiel le plus loue)
export default function StaffHomePage() {
  const { user } = useAuth()
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: dashboardApi.get })
  const d = dashboard.data
  const best = d ? Math.max(1, ...d.revenueByMonth.map((m) => m.cents)) : 1

  return (
    <>
      <PageHeader title={`Bonjour ${user?.firstName ?? ''}`} subtitle={user ? `${ROLE_LABELS[user.role]} · FloreEvent` : undefined} />
      {dashboard.isPending && <Loader />}
      <Alert>{dashboard.isError ? errorMessage(dashboard.error) : ''}</Alert>
      {d && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Tile label="Encaissé ce mois-ci" value={formatEuros(d.revenueThisMonthCents)} detail={`Reste à encaisser : ${formatEuros(d.balanceDueCents)}`} to="/gestion/reservations" Icon={Wallet} />
            <Tile label="Demandes à traiter" value={String(d.pendingRequests)} detail={`${d.quotesAwaiting} devis en attente du client`} to="/gestion/reservations" Icon={ClipboardList} />
            <Tile label="Réservations à venir (30 j)" value={String(d.upcomingBookings)} to="/gestion/planning" Icon={CalendarCheck} />
            <Tile label="Cautions en cours" value={String(d.depositsHeld)} detail={formatEuros(d.depositsHeldCents)} to="/gestion/planning" Icon={ShieldCheck} />
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <h2 className="font-bold">Encaissements des 6 derniers mois</h2>
              <ol className="mt-4 flex h-44 items-end gap-3" aria-label="Encaissements par mois">
                {d.revenueByMonth.map((m) => (
                  <li key={m.month} className="flex h-full flex-1 flex-col items-center justify-end gap-1">
                    <span className="text-xs font-semibold text-slate-600 tabular-nums">{m.cents ? `${Math.round(m.cents / 100)} €` : ''}</span>
                    <span
                      className="w-full max-w-12 rounded-t-md bg-brand-600"
                      style={{ height: `${Math.max(2, (m.cents / best) * 100)}%` }}
                      title={`${m.label} : ${formatEuros(m.cents)}`}
                    />
                    <span className="text-xs text-slate-500">{m.label}</span>
                  </li>
                ))}
              </ol>
            </Card>
            <Card>
              <h2 className="font-bold">Matériel le plus loué</h2>
              {d.topItems.length === 0 ? (
                <p className="mt-3 text-sm text-slate-500">Pas encore de réservation confirmée.</p>
              ) : (
                <ol className="mt-3 space-y-2 text-sm">
                  {d.topItems.map((item, index) => (
                    <li key={item.name} className="flex justify-between gap-3">
                      <span>
                        <span className="mr-2 font-bold text-brand-700">{index + 1}.</span>
                        {item.name}
                      </span>
                      <span className="font-semibold tabular-nums">{item.quantity} loué(s)</span>
                    </li>
                  ))}
                </ol>
              )}
            </Card>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <h2 className="font-bold">Prochaines réservations</h2>
              {d.nextBookings.length === 0 ? (
                <p className="mt-3 text-sm text-slate-500">Aucune réservation confirmée à venir.</p>
              ) : (
                <ul className="mt-3 divide-y divide-stone-100 text-sm">
                  {d.nextBookings.map((b) => (
                    <li key={b.id}>
                      <Link to={`/gestion/reservations/${b.id}`} className="flex flex-wrap justify-between gap-x-3 py-2 hover:text-brand-700">
                        <span className="font-semibold">
                          {b.reference} · {b.customer}
                        </span>
                        <span className="text-slate-500">
                          {formatPeriod(b.startDate, b.startDate)} · {b.deliveryMode === 'LIVRAISON' ? 'livraison' : 'retrait'}
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
            <Card className="space-y-3">
              <h2 className="font-bold">Raccourcis</h2>
              <Link to="/gestion/messages" className="flex items-center gap-2 text-sm font-semibold text-brand-700">
                <Mail className="size-4" aria-hidden />
                Messages du site : {d.unhandledMessages} à traiter
              </Link>
              <Link to="/gestion/planning" className="flex items-center gap-2 text-sm font-semibold text-brand-700">
                <CalendarCheck className="size-4" aria-hidden />
                Planning des livraisons
              </Link>
              <Link to="/gestion/reservations" className="flex items-center gap-2 text-sm font-semibold text-brand-700">
                <FileText className="size-4" aria-hidden />
                Toutes les réservations
              </Link>
            </Card>
          </div>
        </div>
      )}
    </>
  )
}
