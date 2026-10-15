import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { bookingsApi, errorMessage, fieldErrors } from '../../api/client'
import type { Booking, PaymentMethod, ReturnCount } from '../../api/types'
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
    <div className="space-y-6 empty:hidden">
      {b.status === 'CONFIRMEE' && <DeliverForm booking={b} />}
      {b.status === 'EN_COURS' && <ReturnForm booking={b} />}
      {b.status === 'RETOURNEE' && <CloseForm booking={b} />}
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

/** US-16 : le materiel part (livraison ou retrait), la caution est remise. */
function DeliverForm({ booking: b }: { booking: Booking }) {
  const saved = useSaved(b.id)
  const [method, setMethod] = useState<PaymentMethod>('CHEQUE')
  const deliver = useMutation({ mutationFn: () => bookingsApi.deliver(b.id, method), onSuccess: saved })
  const delivery = b.deliveryMode === 'LIVRAISON'
  return (
    <Card>
      <h2 className="font-bold">{delivery ? 'Livraison' : 'Retrait par le client'}</h2>
      <p className="mt-1 text-sm text-slate-600">
        Faites signer le bon de livraison et récupérez la caution de {formatEuros(b.securityDepositCents)} (elle n’est pas encaissée).
      </p>
      <form
        className="mt-4 space-y-4"
        onSubmit={(event) => {
          event.preventDefault()
          deliver.mutate()
        }}
      >
        <Select label="Caution remise par" value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
          {METHODS.map((m) => (
            <option key={m.value} value={m.value}>
              {m.label}
            </option>
          ))}
        </Select>
        <Alert>{deliver.isError ? errorMessage(deliver.error) : ''}</Alert>
        <Button type="submit" loading={deliver.isPending}>
          {delivery ? 'Matériel livré' : 'Matériel remis au client'}
        </Button>
      </form>
    </Card>
  )
}

/** US-17 : etat des lieux au retour, article par article. */
function ReturnForm({ booking: b }: { booking: Booking }) {
  const saved = useSaved(b.id)
  const [counts, setCounts] = useState<ReturnCount[]>(() => b.lines.map((l) => ({ itemId: l.itemId, returned: l.quantity, damaged: 0, missing: 0 })))
  const [note, setNote] = useState('')
  const send = useMutation({ mutationFn: () => bookingsApi.recordReturn(b.id, counts, note), onSuccess: saved })
  const damage = b.lines.reduce((sum, line, i) => sum + (counts[i].damaged + counts[i].missing) * line.replacementValueCents, 0)

  /** Abime ou manquant : le nombre rendu en bon etat s'ajuste tout seul. */
  const change = (index: number, field: 'damaged' | 'missing', value: string) => {
    const quantity = b.lines[index].quantity
    setCounts((all) =>
      all.map((count, i) => {
        if (i !== index) return count
        const next = { ...count, [field]: Math.max(0, Math.min(quantity, Number(value) || 0)) }
        next.returned = Math.max(0, quantity - next.damaged - next.missing)
        return next
      }),
    )
  }

  return (
    <Card>
      <h2 className="font-bold">Retour du matériel</h2>
      <p className="mt-1 text-sm text-slate-600">Comptez chaque article : le matériel abîmé part en réparation, la casse est retenue sur la caution.</p>
      <form
        className="mt-4 space-y-4"
        onSubmit={(event) => {
          event.preventDefault()
          send.mutate()
        }}
      >
        {b.lines.map((line, i) => (
          <fieldset key={line.itemId} className="rounded-xl border border-stone-200 p-3">
            <legend className="px-1 text-sm font-semibold">
              {line.itemName} · {line.quantity} loué(s)
            </legend>
            <div className="grid grid-cols-3 gap-3">
              <Field label="Rendus" value={counts[i].returned} readOnly />
              <Field label="Abîmés" aria-label={`Abîmés : ${line.itemName}`} type="number" min={0} max={line.quantity} value={counts[i].damaged} onChange={(e) => change(i, 'damaged', e.target.value)} />
              <Field label="Manquants" aria-label={`Manquants : ${line.itemName}`} type="number" min={0} max={line.quantity} value={counts[i].missing} onChange={(e) => change(i, 'missing', e.target.value)} />
            </div>
          </fieldset>
        ))}
        <Field label="Remarque (facultatif)" maxLength={255} value={note} onChange={(e) => setNote(e.target.value)} />
        <p className="text-sm">
          Casse à facturer : <strong>{formatEuros(damage)}</strong>
          {damage > 0 && ` (retenue sur la caution : ${formatEuros(Math.min(damage, b.securityDepositCents))})`}
        </p>
        <Alert>{send.isError ? errorMessage(send.error) : ''}</Alert>
        <Button type="submit" loading={send.isPending}>
          Enregistrer le retour
        </Button>
      </form>
    </Card>
  )
}

/** Tout est paye : la caution (moins la casse) est rendue et le dossier est clos. */
function CloseForm({ booking: b }: { booking: Booking }) {
  const saved = useSaved(b.id)
  const close = useMutation({ mutationFn: () => bookingsApi.close(b.id), onSuccess: saved })
  return (
    <Card>
      <h2 className="font-bold">Clôturer le dossier</h2>
      <p className="mt-1 text-sm text-slate-600">
        {b.balanceCents > 0
          ? `Encaissez d’abord le reste à payer (${formatEuros(b.balanceCents)}).`
          : `Rendez la caution au client : ${formatEuros(b.securityDepositReturnedCents ?? 0)}.`}
      </p>
      <div className="mt-4 space-y-3">
        <Alert>{close.isError ? errorMessage(close.error) : ''}</Alert>
        <Button disabled={b.balanceCents > 0} loading={close.isPending} onClick={() => close.mutate()}>
          Caution rendue, clôturer
        </Button>
      </div>
    </Card>
  )
}
