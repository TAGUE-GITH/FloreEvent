import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { bookingsApi, errorMessage, fieldErrors } from '../../api/client'
import type { Booking, PaymentMethod } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { Alert, Button, Card, Field, Select, TextArea } from '../../components/ui'
import { formatDate, formatEuros } from '../../lib/format'
import { centsToInput, parseEuros } from '../../lib/money'

/** Met a jour la reservation affichee et la liste apres une action. */
function useSaved(id: number) {
  const queryClient = useQueryClient()
  return (updated: Booking) => {
    queryClient.setQueryData(['booking', id], updated)
    queryClient.invalidateQueries({ queryKey: ['bookings'] })
  }
}

/** Actions de l'equipe selon l'etape : devis (US-09), paiement hors ligne (US-12), annulation (US-13). */
export function BookingActions({ booking: b }: { booking: Booking }) {
  const { user } = useAuth()
  const sales = user?.role === 'GERANT' || user?.role === 'COMMERCIAL'
  const quoting = b.status === 'DEMANDE' || b.status === 'DEVIS'
  const payable = b.status !== 'DEMANDE' && b.status !== 'CLOTUREE' && b.balanceCents > 0
  const cancellable = quoting || b.status === 'CONFIRMEE'

  return (
    <div className="space-y-6">
      {sales && quoting && <QuoteForm booking={b} />}
      {payable && <PaymentForm booking={b} />}
      {sales && cancellable && <CancelForm booking={b} />}
    </div>
  )
}

function QuoteForm({ booking: b }: { booking: Booking }) {
  const saved = useSaved(b.id)
  const [discount, setDiscount] = useState(b.discountCents ? centsToInput(b.discountCents) : '')
  const [message, setMessage] = useState(b.quoteMessage ?? '')
  const [discountError, setDiscountError] = useState('')
  const send = useMutation({ mutationFn: (discountCents: number) => bookingsApi.sendQuote(b.id, { discountCents, message }), onSuccess: saved })
  const errors = fieldErrors(send.error)
  const gross = b.rentalCents + b.setupCents + b.deliveryCents
  const discountCents = discount.trim() ? parseEuros(discount) : 0

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (discountCents === null) return setDiscountError('Indiquez un montant en euros, par exemple 50 ou 12,50.')
    setDiscountError('')
    send.mutate(discountCents)
  }

  return (
    <Card>
      <h2 className="font-bold">{b.status === 'DEMANDE' ? 'Envoyer le devis' : 'Modifier le devis'}</h2>
      <p className="mt-1 text-sm text-slate-600">
        {b.status === 'DEVIS'
          ? `Devis envoyé, valable jusqu’au ${formatDate(b.quoteValidUntil!)}. Le client peut l’accepter et payer l’acompte en ligne.`
          : 'Vérifiez la demande, accordez une remise si besoin, puis envoyez le devis : le client pourra payer l’acompte en ligne.'}
      </p>
      <form onSubmit={submit} className="mt-4 space-y-4">
        <Field
          label="Remise (€)"
          inputMode="decimal"
          placeholder="0"
          value={discount}
          onChange={(e) => setDiscount(e.target.value)}
          error={discountError || errors.discountCents}
          hint={discountCents ? `Nouveau total : ${formatEuros(gross - discountCents)} TTC` : `Total sans remise : ${formatEuros(gross)} TTC`}
        />
        <TextArea label="Message au client (facultatif)" maxLength={1000} value={message} onChange={(e) => setMessage(e.target.value)} error={errors.message} />
        <Alert>{send.isError && !errors.discountCents ? errorMessage(send.error) : ''}</Alert>
        {send.isSuccess && <Alert type="success">Devis envoyé au client.</Alert>}
        <Button type="submit" loading={send.isPending}>
          {b.status === 'DEMANDE' ? 'Envoyer le devis' : 'Enregistrer le devis'}
        </Button>
      </form>
    </Card>
  )
}

const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'VIREMENT', label: 'Virement' },
  { value: 'CHEQUE', label: 'Chèque' },
  { value: 'ESPECES', label: 'Espèces' },
]

function PaymentForm({ booking: b }: { booking: Booking }) {
  const saved = useSaved(b.id)
  const suggested = b.status === 'DEVIS' ? b.depositCents : b.balanceCents
  const [method, setMethod] = useState<PaymentMethod>('VIREMENT')
  const [amount, setAmount] = useState(centsToInput(suggested))
  const [note, setNote] = useState('')
  const [amountError, setAmountError] = useState('')
  const record = useMutation({
    mutationFn: (amountCents: number) => bookingsApi.recordPayment(b.id, { method, amountCents, note }),
    onSuccess: (updated) => {
      saved(updated)
      setAmount(centsToInput(Math.max(0, updated.balanceCents)))
      setNote('')
    },
  })
  const errors = fieldErrors(record.error)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const cents = parseEuros(amount)
    if (!cents) return setAmountError('Indiquez un montant en euros, par exemple 180 ou 180,50.')
    setAmountError('')
    record.mutate(cents)
  }

  return (
    <Card>
      <h2 className="font-bold">Enregistrer un paiement</h2>
      <p className="mt-1 text-sm text-slate-600">
        {b.status === 'DEVIS'
          ? `Acompte reçu hors ligne : au moins ${formatEuros(b.depositCents)}. La réservation sera confirmée si le matériel est toujours libre.`
          : `Reste à payer : ${formatEuros(b.balanceCents)}.`}
      </p>
      <form onSubmit={submit} className="mt-4 grid gap-4 sm:grid-cols-2">
        <Select label="Moyen de paiement" value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)} error={errors.method}>
          {METHODS.map((m) => (
            <option key={m.value} value={m.value}>
              {m.label}
            </option>
          ))}
        </Select>
        <Field label="Montant (€)" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} error={amountError || errors.amountCents} />
        <Field label="Référence (facultatif)" className="sm:col-span-2" placeholder="N° de chèque, date du virement…" maxLength={255} value={note} onChange={(e) => setNote(e.target.value)} />
        <div className="space-y-3 sm:col-span-2">
          <Alert>{record.isError && !errors.amountCents && !errors.method ? errorMessage(record.error) : ''}</Alert>
          {record.isSuccess && <Alert type="success">Paiement enregistré, facture émise.</Alert>}
          <Button type="submit" loading={record.isPending}>
            Enregistrer le paiement
          </Button>
        </div>
      </form>
    </Card>
  )
}

function CancelForm({ booking: b }: { booking: Booking }) {
  const saved = useSaved(b.id)
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const confirmed = b.status === 'CONFIRMEE'
  const preview = useQuery({ queryKey: ['cancellation', b.id], queryFn: () => bookingsApi.cancellation(b.id), enabled: open && confirmed })
  const cancel = useMutation({ mutationFn: () => bookingsApi.cancel(b.id, reason), onSuccess: saved })
  const errors = fieldErrors(cancel.error)

  if (!open)
    return (
      <Button variant="ghost" onClick={() => setOpen(true)}>
        {confirmed ? 'Annuler la réservation' : 'Ne pas donner suite'}
      </Button>
    )
  return (
    <Card>
      <h2 className="font-bold">{confirmed ? 'Annuler la réservation' : 'Ne pas donner suite'}</h2>
      {confirmed && preview.data && (
        <div className="mt-2 space-y-1 text-sm">
          <p className="text-slate-700">{preview.data.rule}</p>
          <p>
            Gardé : <strong>{formatEuros(preview.data.retainedCents)}</strong>
            {preview.data.refundCents > 0 && (
              <>
                {' '}
                · à rembourser : <strong>{formatEuros(preview.data.refundCents)}</strong>
              </>
            )}
            {preview.data.dueCents > 0 && (
              <>
                {' '}
                · encore dû par le client : <strong>{formatEuros(preview.data.dueCents)}</strong>
              </>
            )}
          </p>
        </div>
      )}
      <form
        className="mt-4 space-y-4"
        onSubmit={(event) => {
          event.preventDefault()
          cancel.mutate()
        }}
      >
        <Field label="Motif (visible par le client)" maxLength={200} value={reason} onChange={(e) => setReason(e.target.value)} error={errors.reason} />
        <Alert>{cancel.isError && !errors.reason ? errorMessage(cancel.error) : ''}</Alert>
        <div className="flex flex-wrap gap-2">
          <Button type="submit" variant="danger" loading={cancel.isPending}>
            Confirmer l’annulation
          </Button>
          <Button type="button" variant="ghost" onClick={() => setOpen(false)}>
            Retour
          </Button>
        </div>
      </form>
    </Card>
  )
}
