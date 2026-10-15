import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation, useParams } from 'react-router-dom'
import { errorMessage, myBookingsApi } from '../../api/client'
import { BookingDetails } from '../../components/BookingDetails'
import { Alert, Badge, Button, Loader } from '../../components/ui'
import { bookingTone } from '../../lib/bookings'
import { formatPeriod } from '../../lib/format'

// Suivi d'une demande ou d'une reservation par le client
export default function ClientBookingPage() {
  const id = Number(useParams().id)
  const sent = (useLocation().state as { sent?: boolean } | null)?.sent
  const queryClient = useQueryClient()
  const booking = useQuery({ queryKey: ['my-booking', id], queryFn: () => myBookingsApi.get(id) })
  const cancel = useMutation({
    mutationFn: () => myBookingsApi.cancel(id),
    onSuccess: (updated) => {
      queryClient.setQueryData(['my-booking', id], updated)
      queryClient.invalidateQueries({ queryKey: ['my-bookings'] })
    },
  })

  if (booking.isPending) return <Loader />
  if (booking.isError)
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <Alert>{errorMessage(booking.error)}</Alert>
      </div>
    )
  const b = booking.data

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <Link to="/mon-espace" className="text-sm font-semibold text-brand-700">
        ← Mon espace
      </Link>
      <div className="mt-3 mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">{b.reference}</h1>
          <p className="text-sm text-slate-500">Location {formatPeriod(b.startDate, b.endDate)}</p>
        </div>
        <Badge tone={bookingTone(b.status)}>{b.statusLabel}</Badge>
      </div>
      {sent && (
        <div className="mb-6">
          <Alert type="success">Merci ! Votre demande est envoyée. Nous vérifions tout et vous envoyons votre devis sous 24 h ouvrées.</Alert>
        </div>
      )}
      <BookingDetails
        booking={b}
        aside={
          (b.status === 'DEMANDE' || b.status === 'DEVIS') && (
            <div className="border-t border-stone-100 pt-3">
              <Alert>{cancel.isError ? errorMessage(cancel.error) : ''}</Alert>
              <Button variant="ghost" className="w-full" loading={cancel.isPending} onClick={() => window.confirm('Annuler cette demande ?') && cancel.mutate()}>
                Annuler ma demande
              </Button>
            </div>
          )
        }
      />
    </div>
  )
}
