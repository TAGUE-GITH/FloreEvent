import { useQuery } from '@tanstack/react-query'
import { ChevronRight, Store, Truck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { bookingsApi, errorMessage } from '../../api/client'
import { Alert, Badge, Card, Loader, PageHeader } from '../../components/ui'
import { bookingTone } from '../../lib/bookings'
import { formatEuros, formatPeriod } from '../../lib/format'

// Demandes de devis et reservations recues (le traitement arrive au sprint 3)
export default function BookingsPage() {
  const bookings = useQuery({ queryKey: ['bookings'], queryFn: bookingsApi.list, refetchInterval: 60_000 })

  return (
    <>
      <PageHeader title="Réservations" subtitle="Demandes de devis envoyées depuis le site et réservations" />
      {bookings.isPending && <Loader />}
      {bookings.isError && <Alert>{errorMessage(bookings.error)}</Alert>}
      {bookings.data?.length === 0 && (
        <Card>
          <p className="text-sm text-slate-500">Aucune demande pour le moment.</p>
        </Card>
      )}
      {bookings.data && bookings.data.length > 0 && (
        <Card className="p-0!">
          <ul className="divide-y divide-stone-100">
            {bookings.data.map((booking) => (
              <li key={booking.id}>
                <Link to={`/gestion/reservations/${booking.id}`} className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 hover:bg-stone-50">
                  <span className="text-slate-500">
                    {booking.deliveryMode === 'LIVRAISON' ? <Truck className="size-5" aria-label="Livraison" /> : <Store className="size-5" aria-label="Retrait" />}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold">
                      {booking.customer} <span className="font-normal text-slate-500">· {booking.reference}</span>
                    </p>
                    <p className="text-sm text-slate-500">
                      {formatPeriod(booking.startDate, booking.endDate)} · {booking.itemCount} article(s)
                    </p>
                  </div>
                  <span className="font-bold tabular-nums">{formatEuros(booking.totalCents)}</span>
                  <Badge tone={bookingTone(booking.status)}>{booking.statusLabel}</Badge>
                  <ChevronRight className="size-4 text-slate-400" aria-hidden />
                </Link>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  )
}
