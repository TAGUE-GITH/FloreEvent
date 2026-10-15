import { useMutation, useQuery } from '@tanstack/react-query'
import { FlaskConical } from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { errorMessage, simulatedPaymentsApi } from '../../api/client'
import { Alert, Button, Loader } from '../../components/ui'
import { formatEuros } from '../../lib/format'

/**
 * Page de paiement de demonstration : elle remplace la page Stripe tant qu'aucune cle Stripe n'est configuree.
 * Aucun argent n'est echange ; la reservation est confirmee par le meme chemin que la confirmation de Stripe.
 */
export default function SimulatedPaymentPage() {
  const session = useSearchParams()[0].get('session') ?? ''
  const navigate = useNavigate()
  const info = useQuery({ queryKey: ['simulated-payment', session], queryFn: () => simulatedPaymentsApi.get(session), enabled: Boolean(session) })
  const pay = useMutation({
    mutationFn: () => simulatedPaymentsApi.pay(session),
    onSuccess: ({ bookingId }) => navigate(`/mon-espace/reservations/${bookingId}?paiement=ok`),
  })

  if (info.isPending && session) return <Loader />
  return (
    <div className="mx-auto max-w-md px-4 py-12">
      <div className="rounded-2xl bg-white p-6 ring-1 ring-stone-200">
        <p className="flex items-center gap-2 text-sm font-semibold text-amber-700">
          <FlaskConical className="size-4" aria-hidden />
          Paiement de démonstration
        </p>
        {info.data ? (
          <>
            <h1 className="mt-2 text-2xl font-extrabold tracking-tight">Acompte {formatEuros(info.data.amountCents)}</h1>
            <p className="mt-1 text-sm text-slate-600">Réservation {info.data.reference}</p>
            <p className="mt-4 rounded-xl bg-amber-50 p-3 text-sm text-amber-900 ring-1 ring-amber-200">
              Le paiement en ligne réel n’est pas encore activé : aucune carte n’est demandée et aucun argent n’est échangé.
            </p>
            <div className="mt-5 space-y-3">
              <Alert>{pay.isError ? errorMessage(pay.error) : ''}</Alert>
              <Button className="w-full" loading={pay.isPending} onClick={() => pay.mutate()}>
                Simuler le paiement de {formatEuros(info.data.amountCents)}
              </Button>
              <Link to={`/mon-espace/reservations/${info.data.bookingId}?paiement=annule`} className="block text-center text-sm font-semibold text-slate-600">
                Annuler et revenir
              </Link>
            </div>
          </>
        ) : (
          <div className="mt-4">
            <Alert>{info.isError ? errorMessage(info.error) : 'Paiement introuvable.'}</Alert>
          </div>
        )}
      </div>
    </div>
  )
}
