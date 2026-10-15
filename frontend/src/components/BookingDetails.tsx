import { Package, Store, Truck } from 'lucide-react'
import type { ReactNode } from 'react'
import { imageUrl } from '../api/client'
import type { Booking } from '../api/types'
import { formatDate, formatEuros, formatPeriod } from '../lib/format'
import { Card } from './ui'

/** Detail d'une reservation (materiel, prix figes, livraison), commun au client et a l'equipe. */
export function BookingDetails({ booking: b, aside }: { booking: Booking; aside?: ReactNode }) {
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
        <Row label="Livraison" value={b.deliveryCents ? formatEuros(b.deliveryCents) : 'Gratuit'} />
        <div className="flex items-baseline justify-between border-t border-stone-100 pt-2">
          <span className="font-bold">Total TTC</span>
          <span className="text-xl font-extrabold text-brand-800 tabular-nums">{formatEuros(b.totalCents)}</span>
        </div>
        <p className="text-right text-xs text-slate-500">dont TVA 20 % : {formatEuros(b.vatCents)}</p>
        <Row label="Acompte (30 %)" value={formatEuros(b.depositCents)} />
        <Row label="Caution" value={formatEuros(b.securityDepositCents)} />
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
