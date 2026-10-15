import { useQuery } from '@tanstack/react-query'
import { Mail, Phone } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { bookingsApi, errorMessage } from '../../api/client'
import { BookingDetails } from '../../components/BookingDetails'
import { Alert, Badge, Loader, PageHeader } from '../../components/ui'
import { bookingTone } from '../../lib/bookings'
import { formatPeriod } from '../../lib/format'
import { BookingActions } from './BookingActions'

// Detail d'une demande ou d'une reservation pour l'equipe
export default function BookingDetailPage() {
  const id = Number(useParams().id)
  const booking = useQuery({ queryKey: ['booking', id], queryFn: () => bookingsApi.get(id) })

  if (booking.isPending) return <Loader />
  if (booking.isError) return <Alert>{errorMessage(booking.error)}</Alert>
  const b = booking.data

  return (
    <>
      <Link to="/gestion/reservations" className="text-sm font-semibold text-brand-700">
        ← Réservations
      </Link>
      <PageHeader title={`${b.reference} · ${b.customer.fullName}`} subtitle={`Location ${formatPeriod(b.startDate, b.endDate)}`} actions={<Badge tone={bookingTone(b.status)}>{b.statusLabel}</Badge>} />
      <div className="mb-6 max-w-3xl">
        <BookingActions booking={b} />
      </div>
      <BookingDetails
        booking={b}
        filesPath={`/bookings/${b.id}`}
        aside={
          <div className="space-y-1.5 border-t border-stone-100 pt-3">
            <p className="font-bold">Client</p>
            <p>
              {b.customer.fullName}
              {b.customer.company ? ` · ${b.customer.company}` : ''}
            </p>
            <a href={`tel:${b.customer.phone}`} className="flex items-center gap-2 font-semibold text-brand-700">
              <Phone className="size-4" aria-hidden />
              {b.customer.phone}
            </a>
            {b.customer.email && (
              <a href={`mailto:${b.customer.email}`} className="flex items-center gap-2 font-semibold text-brand-700">
                <Mail className="size-4" aria-hidden />
                {b.customer.email}
              </a>
            )}
          </div>
        }
      />
    </>
  )
}
