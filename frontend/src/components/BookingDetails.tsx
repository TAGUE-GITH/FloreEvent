import { FileText, Package, Store, Truck } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { downloadFile, errorMessage, imageUrl } from '../api/client'
import type { Booking } from '../api/types'
import { formatDate, formatEuros, formatPeriod } from '../lib/format'
import { Card } from './ui'

/** Detail d'une reservation (materiel, prix figes, livraison), commun au client et a l'equipe. */
export function BookingDetails({ booking: b, aside, filesPath }: { booking: Booking; aside?: ReactNode; filesPath: string }) {
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
      <div className="min-w-0 space-y-6">
        <Card className="p-0!">
          <ul className="divide-y divide-stone-100">
            {b.lines.map((line) => (
              <li key={line.itemId} className="flex items-center gap-3 p-4">
                {line.imageUrl ? (
                  <img src={imageUrl(line.imageUrl)} alt="" className="size-14 shrink-0 rounded-lg object-cover" />
                ) : (
                  <span className="flex size-14 shrink-0 items-center justify-center rounded-lg bg-stone-100 text-stone-400">
                    <Package className="size-5" aria-hidden />
                  </span>
                )}
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">{line.itemName}</p>
                  <p className="text-sm text-slate-500 tabular-nums">
                    {line.quantity} × {formatEuros(line.dayPriceCents)} / jour
                  </p>
                  {line.returnedQuantity != null && (
                    <p className="text-sm text-slate-600">
                      Retour : {line.returnedQuantity} rendu(s)
                      {line.damagedQuantity ? ` · ${line.damagedQuantity} abîmé(s)` : ''}
                      {line.missingQuantity ? ` · ${line.missingQuantity} manquant(s)` : ''}
                    </p>
                  )}
                </div>
                <p className="font-bold tabular-nums">{formatEuros(line.lineTotalCents)}</p>
              </li>
            ))}
          </ul>
        </Card>
        <Card className="space-y-2 text-sm">
          <p className="flex items-center gap-2 font-semibold">
            {b.deliveryMode === 'LIVRAISON' ? <Truck className="size-4 text-brand-700" aria-hidden /> : <Store className="size-4 text-brand-700" aria-hidden />}
            {b.deliveryMode === 'LIVRAISON' ? `Livraison et reprise${b.installation ? ', avec installation' : ''}` : 'Retrait à l’entrepôt de Melun'}
          </p>
          {b.address && (
            <p className="text-slate-600">
              {b.address} ({b.distanceKm} km)
            </p>
          )}
          {b.note && <p className="text-slate-600">« {b.note} »</p>}
        </Card>
        {b.quoteMessage && (
          <Card className="text-sm">
            <h2 className="mb-1 font-bold">Message de FloreEvent</h2>
            <p className="text-slate-700">{b.quoteMessage}</p>
          </Card>
        )}
        <Card>
          <h2 className="mb-2 font-bold">Historique</h2>
          <ol className="space-y-1.5 text-sm">
            {b.events.map((event, index) => (
              <li key={index} className="flex flex-wrap gap-x-2">
                <span className="font-semibold">{event.label}</span>
                <span className="text-slate-500">
                  le {formatDate(event.createdAt)}
                  {event.author ? ` · ${event.author}` : ''}
                  {event.note ? ` · ${event.note}` : ''}
                </span>
              </li>
            ))}
          </ol>
        </Card>
      </div>
      <Card className="h-fit space-y-2 text-sm">
        <h2 className="mb-1 font-bold">Montant</h2>
        <p className="text-slate-600">Location {formatPeriod(b.startDate, b.endDate)}</p>
        <Row label={`Location (${b.days} jour${b.days > 1 ? 's' : ''})`} value={formatEuros(b.rentalCents)} />
        {b.setupCents > 0 && <Row label="Installation" value={formatEuros(b.setupCents)} />}
        {b.prestations.map((p) => (
          <Row key={p.id} label={p.name} value={formatEuros(p.priceCents)} />
        ))}
        <Row label="Livraison" value={b.deliveryCents ? formatEuros(b.deliveryCents) : 'Gratuit'} />
        {b.discountCents > 0 && <Row label="Remise" value={`− ${formatEuros(b.discountCents)}`} />}
        <div className="flex items-baseline justify-between border-t border-stone-100 pt-2">
          <span className="font-bold">Total TTC</span>
          <span className="text-xl font-extrabold text-brand-800 tabular-nums">{formatEuros(b.totalCents)}</span>
        </div>
        <p className="text-right text-xs text-slate-500">dont TVA 20 % : {formatEuros(b.vatCents)}</p>
        <Row label="Acompte (30 %)" value={formatEuros(b.depositCents)} />
        {b.damageCents > 0 && <Row label="Casse et manquants" value={formatEuros(b.damageCents)} />}
        <Row label="Caution" value={formatEuros(b.securityDepositCents)} />
        {b.securityDepositReturnedCents !== undefined && <Row label="Caution rendue" value={formatEuros(b.securityDepositReturnedCents)} />}
        <Payments booking={b} />
        <Documents booking={b} filesPath={filesPath} />
        {aside}
      </Card>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <span className="text-slate-600">{label}</span>
      <span className="font-semibold tabular-nums">{value}</span>
    </div>
  )
}

const CONFIRMED = ['CONFIRMEE', 'EN_COURS', 'RETOURNEE', 'CLOTUREE']

/** Paiements recus et reste a payer. */
function Payments({ booking: b }: { booking: Booking }) {
  const received = b.payments.filter((p) => p.status !== 'EN_ATTENTE')
  if (received.length === 0 && b.status !== 'ANNULEE') return null
  return (
    <div className="space-y-1.5 border-t border-stone-100 pt-3">
      <p className="font-bold">Paiements</p>
      {received.map((p) => (
        <div key={p.id} className="flex justify-between gap-3">
          <span className="text-slate-600">
            {p.methodLabel} · {formatDate(p.paidAt ?? p.createdAt)}
            {p.refundedCents > 0 && ` · remboursé ${formatEuros(p.refundedCents)}`}
          </span>
          <span className="font-semibold tabular-nums">{formatEuros(p.amountCents)}</span>
        </div>
      ))}
      {b.status === 'ANNULEE' && b.cancellationFeeCents !== undefined && <Row label="Frais d’annulation" value={formatEuros(b.cancellationFeeCents)} />}
      <Row label="Déjà payé" value={formatEuros(b.paidCents)} />
      <div className="flex justify-between gap-3">
        <span className="font-bold">Reste à payer</span>
        <span className="font-extrabold tabular-nums">{formatEuros(Math.max(0, b.balanceCents))}</span>
      </div>
    </div>
  )
}

/** Devis, confirmation et factures en PDF. */
function Documents({ booking: b, filesPath }: { booking: Booking; filesPath: string }) {
  const [error, setError] = useState('')
  const files: { label: string; path: string; name: string }[] = []
  if (b.quoteValidUntil) files.push({ label: 'Devis', path: `${filesPath}/documents/devis`, name: `Devis-${b.reference}.pdf` })
  if (CONFIRMED.includes(b.status)) {
    files.push({ label: 'Confirmation de réservation', path: `${filesPath}/documents/confirmation`, name: `Confirmation-${b.reference}.pdf` })
    files.push({ label: 'Bon de livraison', path: `${filesPath}/documents/bon-de-livraison`, name: `Bon-de-livraison-${b.reference}.pdf` })
  }
  for (const invoice of b.invoices)
    files.push({ label: `${invoice.kindLabel} ${invoice.number}`, path: `${filesPath}/invoices/${invoice.id}`, name: `${invoice.number}.pdf` })
  if (files.length === 0) return null

  const download = (path: string, name: string) => {
    setError('')
    downloadFile(path, name).catch((e) => setError(errorMessage(e)))
  }
  return (
    <div className="space-y-1 border-t border-stone-100 pt-3">
      <p className="font-bold">Documents</p>
      {files.map((file) => (
        <button
          key={file.path}
          type="button"
          onClick={() => download(file.path, file.name)}
          className="flex w-full items-center gap-2 rounded-lg py-1 text-left font-semibold text-brand-700 hover:text-brand-900"
        >
          <FileText className="size-4 shrink-0" aria-hidden />
          {file.label} (PDF)
        </button>
      ))}
      {error && <p className="text-sm text-red-700">{error}</p>}
    </div>
  )
}
