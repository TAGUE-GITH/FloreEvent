import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CreditCard } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom'
import { errorMessage, myBookingsApi } from '../../api/client'
import type { Booking } from '../../api/types'
import { BookingDetails } from '../../components/BookingDetails'
import { Alert, Badge, Button, Loader } from '../../components/ui'
import { bookingTone } from '../../lib/bookings'
import { formatDate, formatEuros, formatPeriod } from '../../lib/format'

// Suivi d'une demande ou d'une reservation par le client : devis, paiement de l'acompte, documents, annulation
export default function ClientBookingPage() {
  const id = Number(useParams().id)
  const sent = (useLocation().state as { sent?: boolean } | null)?.sent
  // Retour de la page de paiement : ?paiement=ok ou ?paiement=annule
  const payment = useSearchParams()[0].get('paiement')
  const booking = useQuery({
    queryKey: ['my-booking', id],
    queryFn: () => myBookingsApi.get(id),
    // La confirmation de Stripe arrive quelques secondes apres le retour du client : on attend qu'elle arrive
    refetchInterval: (query) => (payment === 'ok' && query.state.data?.status === 'DEVIS' ? 2000 : false),
  })

  if (booking.isPending) return <Loader />
  if (booking.isError)
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <Alert>{errorMessage(booking.error)}</Alert>
      </div>
    )
  const b = booking.data
  const waitingForPayment = payment === 'ok' && b.status === 'DEVIS'

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
      <div className="mb-6 space-y-3 empty:hidden">
        {sent && <Alert type="success">Merci ! Votre demande est envoyée. Nous vérifions tout et vous envoyons votre devis sous 24 h ouvrées.</Alert>}
        {waitingForPayment && <Alert type="success">Paiement reçu, confirmation de votre réservation en cours…</Alert>}
        {payment === 'ok' && b.status === 'CONFIRMEE' && (
          <Alert type="success">Merci ! Votre acompte est payé : votre réservation est confirmée. Votre facture est disponible ci-dessous.</Alert>
        )}
        {payment === 'annule' && b.status === 'DEVIS' && <Alert>Paiement annulé : aucun montant n’a été prélevé. Vous pouvez réessayer quand vous voulez.</Alert>}
      </div>
      {b.status === 'DEVIS' && !waitingForPayment && <AcceptQuote booking={b} />}
      <BookingDetails booking={b} filesPath={`/me/bookings/${b.id}`} aside={<CancelBooking booking={b} />} />
    </div>
  )
}

/** US-10 : le client accepte le devis et paie l'acompte sur la page de paiement securisee. */
function AcceptQuote({ booking: b }: { booking: Booking }) {
  const checkout = useMutation({
    mutationFn: () => myBookingsApi.checkout(b.id),
    onSuccess: ({ url }) => window.location.assign(url),
  })

  return (
    <section className="mb-6 rounded-2xl bg-brand-50 p-5 ring-1 ring-brand-200">
      <h2 className="text-lg font-bold text-brand-900">Votre devis est prêt</h2>
      {b.quoteExpired ? (
        <p className="mt-1 text-sm text-slate-700">Ce devis a expiré le {formatDate(b.quoteValidUntil!)}. Contactez-nous pour le renouveler.</p>
      ) : (
        <>
          <p className="mt-1 text-sm text-slate-700">
            Total {formatEuros(b.totalCents)} TTC. Pour confirmer votre réservation, payez l’acompte de <strong>{formatEuros(b.depositCents)}</strong>{' '}
            avant le {formatDate(b.quoteValidUntil!)}. Le solde sera à régler au plus tard à la livraison.
          </p>
          <div className="mt-4 space-y-3">
            <Alert>{checkout.isError ? errorMessage(checkout.error) : ''}</Alert>
            <Button loading={checkout.isPending || checkout.isSuccess} onClick={() => checkout.mutate()}>
              <CreditCard className="size-4" aria-hidden />
              Accepter le devis et payer l’acompte ({formatEuros(b.depositCents)})
            </Button>
            <p className="text-xs text-slate-500">Paiement par carte sur une page sécurisée : FloreEvent ne voit jamais votre numéro de carte.</p>
          </div>
        </>
      )}
    </section>
  )
}

/** US-13 : annulation ; pour une reservation confirmee, la regle selon la date est montree avant de confirmer. */
function CancelBooking({ booking: b }: { booking: Booking }) {
  const queryClient = useQueryClient()
  const [asked, setAsked] = useState(false)
  const confirmed = b.status === 'CONFIRMEE'
  const preview = useQuery({ queryKey: ['my-cancellation', b.id], queryFn: () => myBookingsApi.cancellation(b.id), enabled: asked && confirmed })
  const cancel = useMutation({
    mutationFn: () => myBookingsApi.cancel(b.id),
    onSuccess: (updated) => {
      queryClient.setQueryData(['my-booking', b.id], updated)
      queryClient.invalidateQueries({ queryKey: ['my-bookings'] })
    },
  })

  if (b.status !== 'DEMANDE' && b.status !== 'DEVIS' && !confirmed) return null
  if (!confirmed)
    return (
      <div className="border-t border-stone-100 pt-3">
        <Alert>{cancel.isError ? errorMessage(cancel.error) : ''}</Alert>
        <Button variant="ghost" className="w-full" loading={cancel.isPending} onClick={() => window.confirm('Annuler cette demande ?') && cancel.mutate()}>
          Annuler ma demande
        </Button>
      </div>
    )
  return (
    <div className="space-y-2 border-t border-stone-100 pt-3">
      {!asked ? (
        <Button variant="ghost" className="w-full" onClick={() => setAsked(true)}>
          Annuler ma réservation
        </Button>
      ) : (
        <>
          <p className="font-bold">Annuler ma réservation</p>
          {preview.isPending && <p className="text-slate-500">Calcul des conditions…</p>}
          {preview.data && (
            <>
              <p className="text-slate-700">{preview.data.rule}</p>
              {preview.data.refundCents > 0 && <p>Vous serez remboursé de {formatEuros(preview.data.refundCents)}.</p>}
              {preview.data.retainedCents > 0 && <p>FloreEvent conserve {formatEuros(preview.data.retainedCents)}.</p>}
              {preview.data.dueCents > 0 && <p>Il vous restera {formatEuros(preview.data.dueCents)} à régler.</p>}
            </>
          )}
          <Alert>{cancel.isError ? errorMessage(cancel.error) : ''}</Alert>
          <Button variant="danger" className="w-full" loading={cancel.isPending} disabled={!preview.data} onClick={() => cancel.mutate()}>
            Confirmer l’annulation
          </Button>
          <Button variant="ghost" className="w-full" onClick={() => setAsked(false)}>
            Garder ma réservation
          </Button>
        </>
      )}
    </div>
  )
}
