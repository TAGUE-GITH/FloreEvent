import { useQuery } from '@tanstack/react-query'
import { CalendarCheck, ChevronRight, LogOut, Plus, UserRound } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { errorMessage, myBookingsApi } from '../../api/client'
import { useAuth } from '../../auth/AuthProvider'
import { buttonClasses } from '../../components/buttonClasses'
import { Alert, Badge, Button, Card, Loader } from '../../components/ui'
import { bookingTone } from '../../lib/bookings'
import { formatEuros, formatPeriod } from '../../lib/format'
import { ProfilePanel } from '../profile/ProfilePanel'

// US-08 : « Mon espace » du client : ses demandes de devis et reservations, son profil
export default function ClientSpacePage() {
  const { user, logout } = useAuth()
  const welcome = (useLocation().state as { welcome?: boolean } | null)?.welcome
  const bookings = useQuery({ queryKey: ['my-bookings'], queryFn: myBookingsApi.list })

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Mon espace</h1>
          <p className="mt-1 text-slate-600">Bonjour {user?.firstName}, bienvenue chez FloreEvent.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/mon-espace/profil" className={buttonClasses('secondary')}>
            <UserRound className="size-4" aria-hidden />
            Mon profil
          </Link>
          <Button variant="ghost" onClick={logout}>
            <LogOut className="size-4" aria-hidden />
            Se déconnecter
          </Button>
        </div>
      </div>
      {welcome && (
        <div className="mb-6">
          <Alert type="success">Votre compte est créé. Composez votre sélection dans le catalogue, puis envoyez votre demande de devis.</Alert>
        </div>
      )}
      <Card>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="flex items-center gap-2 font-bold">
            <CalendarCheck className="size-5 text-brand-700" aria-hidden />
            Mes demandes et réservations
          </h2>
          <Link to="/catalogue" className={buttonClasses('primary', 'px-3 py-2')}>
            <Plus className="size-4" aria-hidden />
            Nouvelle demande
          </Link>
        </div>
        {bookings.isPending && <Loader />}
        <Alert>{bookings.isError ? errorMessage(bookings.error) : ''}</Alert>
        {bookings.data?.length === 0 && <p className="mt-3 text-sm text-slate-600">Aucune demande pour le moment.</p>}
        {bookings.data && bookings.data.length > 0 && (
          <ul className="mt-2 divide-y divide-stone-100">
            {bookings.data.map((booking) => (
              <li key={booking.id}>
                <Link to={`/mon-espace/reservations/${booking.id}`} className="flex flex-wrap items-center gap-x-3 gap-y-1 py-3 hover:bg-stone-50">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold">
                      {booking.reference} <span className="font-normal text-slate-500">· {formatPeriod(booking.startDate, booking.endDate)}</span>
                    </p>
                    <p className="text-sm text-slate-500 tabular-nums">
                      {booking.itemCount} article(s) · {formatEuros(booking.totalCents)}
                    </p>
                  </div>
                  <Badge tone={bookingTone(booking.status)}>{booking.statusLabel}</Badge>
                  <ChevronRight className="size-4 text-slate-400" aria-hidden />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
      <h2 className="mt-10 mb-4 text-xl font-bold">Mon profil</h2>
      <ProfilePanel />
    </div>
  )
}
